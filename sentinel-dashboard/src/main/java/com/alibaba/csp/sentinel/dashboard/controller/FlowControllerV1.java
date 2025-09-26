/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.controller;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.alibaba.csp.sentinel.dashboard.auth.AuthAction;
import com.alibaba.csp.sentinel.dashboard.auth.AuthService.PrivilegeType;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.repository.rule.nacos.NacosRuleRepositoryAdapter;
import com.alibaba.csp.sentinel.util.StringUtil;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.domain.Result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Flow rule controller.
 *
 * @author leyou
 * @author Eric Zhao
 */
@RestController
@RequestMapping(value = "/v1/flow")
public class FlowControllerV1 {

    private final Logger logger = LoggerFactory.getLogger(FlowControllerV1.class);

    @Autowired
    private NacosRuleRepositoryAdapter<FlowRuleEntity> repository;
    @Autowired
    private AppManagement appManagement;

    @GetMapping("/rules")
    @AuthAction(PrivilegeType.READ_RULE)
    public Result<List<FlowRuleEntity>> apiQueryMachineRules(@RequestParam String app,
                                                             @RequestParam String ip,
                                                             @RequestParam Integer port) {
        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(-1, "app can't be null or empty");
        }
        if (StringUtil.isEmpty(ip)) {
            return Result.ofFail(-1, "ip can't be null or empty");
        }
        if (port == null) {
            return Result.ofFail(-1, "port can't be null");
        }
        if (!appManagement.isValidMachineOfApp(app, ip)) {
            return Result.ofFail(-1, "given ip does not belong to given app");
        }
        try {
            List<FlowRuleEntity> rules = repository.findAllByMachine(MachineInfo.of(app, ip, port));
            return Result.ofSuccess(rules);
        } catch (Throwable throwable) {
            logger.error("Error when querying flow rules", throwable);
            return Result.ofThrowable(-1, throwable);
        }
    }

    private <R> Result<R> checkEntityInternal(FlowRuleEntity entity) {
        if (StringUtil.isBlank(entity.getApp())) {
            return Result.ofFail(-1, "app can't be null or empty");
        }
        if (StringUtil.isBlank(entity.getIp())) {
            return Result.ofFail(-1, "ip can't be null or empty");
        }
        if (entity.getPort() == null) {
            return Result.ofFail(-1, "port can't be null");
        }
//        if (!appManagement.isValidMachineOfApp(entity.getApp(), entity.getIp())) {
//            return Result.ofFail(-1, "given ip does not belong to given app");
//        }
        if (StringUtil.isBlank(entity.getLimitApp())) {
            return Result.ofFail(-1, "limitApp can't be null or empty");
        }
        if (StringUtil.isBlank(entity.getResource())) {
            return Result.ofFail(-1, "resource can't be null or empty");
        }
        if (entity.getGrade() == null) {
            return Result.ofFail(-1, "grade can't be null");
        }
        if (entity.getGrade() != 0 && entity.getGrade() != 1) {
            return Result.ofFail(-1, "grade must be 0 or 1, but " + entity.getGrade() + " got");
        }
        if (entity.getCount() == null || entity.getCount() < 0) {
            return Result.ofFail(-1, "count should be at lease zero");
        }
        if (entity.getStrategy() == null) {
            return Result.ofFail(-1, "strategy can't be null");
        }
        if (entity.getStrategy() != 0 && StringUtil.isBlank(entity.getRefResource())) {
            return Result.ofFail(-1, "refResource can't be null or empty when strategy!=0");
        }
        if (entity.getControlBehavior() == null) {
            return Result.ofFail(-1, "controlBehavior can't be null");
        }
        int controlBehavior = entity.getControlBehavior();
        if (controlBehavior == 1 && entity.getWarmUpPeriodSec() == null) {
            return Result.ofFail(-1, "warmUpPeriodSec can't be null when controlBehavior==1");
        }
        if (controlBehavior == 2 && entity.getMaxQueueingTimeMs() == null) {
            return Result.ofFail(-1, "maxQueueingTimeMs can't be null when controlBehavior==2");
        }
        if (entity.isClusterMode() && entity.getClusterConfig() == null) {
            return Result.ofFail(-1, "cluster config should be valid");
        }
        return null;
    }

    @PostMapping("/rule")
    @AuthAction(PrivilegeType.WRITE_RULE)
    public Result<FlowRuleEntity> apiAddFlowRule(@RequestBody FlowRuleEntity entity) {
        Result<FlowRuleEntity> checkResult = checkEntityInternal(entity);
        if (checkResult != null) {
            return checkResult;
        }
        entity.setId(null);
        Date date = new Date();
        entity.setGmtCreate(date);
        entity.setGmtModified(date);
        entity.setLimitApp(entity.getLimitApp().trim());
        entity.setResource(entity.getResource().trim());
        try {
            entity = repository.save(entity);

            return Result.ofSuccess(entity);
        } catch (Throwable t) {
            Throwable e = t instanceof ExecutionException ? t.getCause() : t;
            logger.error("Failed to add new flow rule, app={}, ip={}", entity.getApp(), entity.getIp(), e);
            return Result.ofFail(-1, e.getMessage());
        }
    }

    @PutMapping("/save.json")
    @AuthAction(PrivilegeType.WRITE_RULE)
    public Result<FlowRuleEntity> apiUpdateFlowRule(@RequestBody FlowRuleEntity entity) {
        if (entity == null) {
            return Result.ofFail(-1, "entity can't be null");
        }
        if (entity.getId() == null) {
            return Result.ofFail(-1, "id can't be null");
        }
        if (StringUtil.isNotBlank(entity.getApp())) {
            entity.setApp(entity.getApp().trim());
        }
        if (StringUtil.isNotBlank(entity.getLimitApp())) {
            entity.setLimitApp(entity.getLimitApp().trim());
        }
        if (StringUtil.isNotBlank(entity.getResource())) {
            entity.setResource(entity.getResource().trim());
        }
        if (entity.getGrade() != null) {
            if (entity.getGrade() != 0 && entity.getGrade() != 1) {
                return Result.ofFail(-1, "grade must be 0 or 1, but " + entity.getGrade() + " got");
            }
            entity.setGrade(entity.getGrade());
        }
        if (entity.getStrategy() != null) {
            if (entity.getStrategy() != 0 && entity.getStrategy() != 1 && entity.getStrategy() != 2) {
                return Result.ofFail(-1, "strategy must be in [0, 1, 2], but " + entity.getStrategy() + " got");
            }
            entity.setStrategy(entity.getStrategy());
            if (entity.getStrategy() != 0) {
                if (StringUtil.isBlank(entity.getRefResource())) {
                    return Result.ofFail(-1, "refResource can't be null or empty when strategy!=0");
                }
                entity.setRefResource(entity.getRefResource().trim());
            }
        }
        if (entity.getControlBehavior() != null) {
            if (entity.getControlBehavior() != 0 && entity.getControlBehavior() != 1 && entity.getControlBehavior() != 2) {
                return Result.ofFail(-1, "controlBehavior must be in [0, 1, 2], but " + entity.getControlBehavior() + " got");
            }
            if (entity.getControlBehavior() == 1 && entity.getWarmUpPeriodSec() == null) {
                return Result.ofFail(-1, "warmUpPeriodSec can't be null when controlBehavior==1");
            }
            if (entity.getControlBehavior() == 2 && entity.getMaxQueueingTimeMs() == null) {
                return Result.ofFail(-1, "maxQueueingTimeMs can't be null when controlBehavior==2");
            }
            entity.setControlBehavior(entity.getControlBehavior());
            if (entity.getWarmUpPeriodSec() != null) {
                entity.setWarmUpPeriodSec(entity.getWarmUpPeriodSec());
            }
            if (entity.getMaxQueueingTimeMs() != null) {
                entity.setMaxQueueingTimeMs(entity.getMaxQueueingTimeMs());
            }
        }
        Date date = new Date();
        entity.setGmtModified(date);
        try {
            entity = repository.save(entity);
            if (entity == null) {
                return Result.ofFail(-1, "save entity fail: null");
            }

            return Result.ofSuccess(entity);
        } catch (Throwable t) {
            Throwable e = t instanceof ExecutionException ? t.getCause() : t;
            logger.error("Error when updating flow rules, app={}, ip={}, ruleId={}", entity.getApp(),
                entity.getIp(), entity.getId(), e);
            return Result.ofFail(-1, e.getMessage());
        }
    }

    @DeleteMapping("/delete.json")
    @AuthAction(PrivilegeType.WRITE_RULE)
    public Result<Long> apiDeleteFlowRule(Long id) {

        if (id == null) {
            return Result.ofFail(-1, "id can't be null");
        }
        FlowRuleEntity oldEntity = repository.findById(id);
        if (oldEntity == null) {
            return Result.ofSuccess(null);
        }

        try {
            repository.delete(id);
            return Result.ofSuccess(id);
        } catch (Exception e) {
            logger.error("Error when deleting flow rules, app={}, ip={}, id={}", oldEntity.getApp(),
                    oldEntity.getIp(), id, e);
            return Result.ofFail(-1, e.getMessage());
        }
    }
}
