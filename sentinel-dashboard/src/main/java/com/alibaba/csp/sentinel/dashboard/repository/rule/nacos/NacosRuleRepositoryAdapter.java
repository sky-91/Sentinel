package com.alibaba.csp.sentinel.dashboard.repository.rule.nacos;

import com.alibaba.csp.sentinel.dashboard.config.NacosConfig;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.RuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.repository.rule.RuleRepository;
import com.alibaba.csp.sentinel.dashboard.util.NacosConfigUtil;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.ConfigFactory;
import com.alibaba.nacos.api.config.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class NacosRuleRepositoryAdapter<T extends RuleEntity> implements RuleRepository<T, Long> {

    private static final Logger logger = LoggerFactory.getLogger(NacosRuleRepositoryAdapter.class);

    @Autowired
    private NacosConfig nacosConfig;

    private String dataIdPrefix;
    private String group;
    private String serverAddr;

    private final Map<Long, T> entityMap = new ConcurrentHashMap<>();

    private final Class<T> ruleClass;

    private final ConcurrentHashMap<String, AtomicLong> appIdGenerators = new ConcurrentHashMap<>();

    public NacosRuleRepositoryAdapter(Class<T> ruleClass) {
        Assert.notNull(ruleClass, "ruleClass cannot be null");
        this.ruleClass = ruleClass;
    }

    private static final Map<String, String> PREFIX_MAP;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("Flow", NacosConfigUtil.FLOW_DATA_ID_POSTFIX);
        map.put("Degrade", NacosConfigUtil.DEGRADE_DATA_ID_POSTFIX);
        map.put("ParamFlow", NacosConfigUtil.PARAM_FLOW_DATA_ID_POSTFIX);
        map.put("System", NacosConfigUtil.SYSTEM_DATA_ID_POSTFIX);
        map.put("Authority", NacosConfigUtil.AUTHORITY_DATA_ID_POSTFIX);
        map.put("GatewayFlow", NacosConfigUtil.GATEWAY_FLOW_DATA_ID_POSTFIX);
        map.put("ApiDefinition", NacosConfigUtil.API_DEFINITION_DATA_ID_POSTFIX);
        PREFIX_MAP = Collections.unmodifiableMap(map);
    }

    @PostConstruct
    public void init() {
        this.serverAddr = nacosConfig.getServerAddr();
        this.group = nacosConfig.getGroupId();
        // 根据规则类型设置 dataId 前缀
        String simpleName = ruleClass.getSimpleName();

        // 在 Map 里找第一个匹配的 key
        this.dataIdPrefix = PREFIX_MAP.entrySet().stream()
                .filter(e -> simpleName.startsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(simpleName.toLowerCase().replace("entity", "") + "-rules");
    }

    private String getDataId(String appName) {
        return appName + dataIdPrefix;
    }

    private List<T> loadRulesFromNacos(String appName) {
        try {
            ConfigService configService = ConfigFactory.createConfigService(serverAddr);
            String content = configService.getConfig(getDataId(appName), group, 3000);
            if (StringUtil.isEmpty(content)) {
                return new ArrayList<>();
            }
            return JSON.parseArray(content, ruleClass);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private void saveRulesToNacos(String appName, List<T> rules) {
        try {
            ConfigService configService = ConfigFactory.createConfigService(serverAddr);
            String content = JSON.toJSONString(rules);
            configService.publishConfig(getDataId(appName), group, content);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public T save(T entity) {
        AssertUtil.notNull(entity, "entity cannot be null");
        String appName = entity.getApp();
        if (entity.getId() == null) {
            entity.setId(getIdGenerator(appName).getAndIncrement());
        }

        List<T> rules = loadRulesFromNacos(appName);
        // 替换或新增
        boolean found = false;

        for (int i = 0; i < rules.size(); i++) {
            if (Objects.equals(rules.get(i).getId(), entity.getId())) {
                rules.set(i, entity);
                found = true;
                break;
            }
        }
        if (!found) {
            rules.add(entity);
        }
        saveRulesToNacos(appName, rules);
        entityMap.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public List<T> saveAll(List<T> rules) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }
        String appName = rules.get(0).getApp();
        List<T> existingRules = loadRulesFromNacos(appName);
        Map<Long, T> ruleMap = existingRules.stream()
                .collect(Collectors.toMap(RuleEntity::getId, r -> r, (r1, r2) -> r2));

        for (T rule : rules) {
            if (rule.getId() == null) {
                rule.setId(getIdGenerator(appName).getAndIncrement());
            }
            ruleMap.put(rule.getId(), rule);
        }

        List<T> merged = new ArrayList<>(ruleMap.values());
        saveRulesToNacos(appName, merged);
        merged.forEach(r -> entityMap.put(r.getId(), r));
        return merged;
    }

    @Override
    public T delete(Long id) {
        T entity = entityMap.get(id);
        if (entity != null) {
            String appName = entity.getApp();
            List<T> rules = loadRulesFromNacos(appName);
            rules.removeIf(r -> Objects.equals(r.getId(), id));
            saveRulesToNacos(appName, rules);
            entityMap.remove(id);
        }
        return entity;
    }

    @Override
    public T findById(Long id) {
        return entityMap.get(id);
    }

    @Override
    public List<T> findAllByApp(String appName) {
        List<T> rules = loadRulesFromNacos(appName);
        // 同步到内存缓存
        entityMap.clear();
        rules.forEach(r -> entityMap.put(r.getId(), r));
        return rules;
    }

    @Override
    public List<T> findAllByMachine(MachineInfo machineInfo) {
        String app = machineInfo.getApp();
        if (StringUtil.isEmpty(app)) {
            return new ArrayList<>();
        }

        return findAllByApp(app);
    }

    private AtomicLong getIdGenerator(String appName) {
        return appIdGenerators.computeIfAbsent(appName, k -> {
            List<T> rules = loadRulesFromNacos(k);
            long maxId = rules.stream().mapToLong(RuleEntity::getId).max().orElse(0L);
            return new AtomicLong(maxId + 1);
        });
    }
}
