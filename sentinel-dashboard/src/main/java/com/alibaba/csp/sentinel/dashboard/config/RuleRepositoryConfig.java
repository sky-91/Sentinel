package com.alibaba.csp.sentinel.dashboard.config;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.ApiDefinitionEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.GatewayFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.AuthorityRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.DegradeRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ParamFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.SystemRuleEntity;
import com.alibaba.csp.sentinel.dashboard.repository.rule.nacos.NacosRuleRepositoryAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuleRepositoryConfig {

    @Bean
    public NacosRuleRepositoryAdapter<FlowRuleEntity> flowRuleNacosRepository() {
        return new NacosRuleRepositoryAdapter<>(FlowRuleEntity.class);
    }

    @Bean
    public NacosRuleRepositoryAdapter<DegradeRuleEntity> degradeRuleNacosRepository() {
        return new NacosRuleRepositoryAdapter<>(DegradeRuleEntity.class);
    }

    @Bean
    public NacosRuleRepositoryAdapter<ParamFlowRuleEntity> paramFlowRuleNacosRepository() {
        return new NacosRuleRepositoryAdapter<>(ParamFlowRuleEntity.class);
    }

    @Bean
    public NacosRuleRepositoryAdapter<SystemRuleEntity> systemRuleNacosRepository() {
        return new NacosRuleRepositoryAdapter<>(SystemRuleEntity.class);
    }

    @Bean
    public NacosRuleRepositoryAdapter<AuthorityRuleEntity> authorityRuleNacosRepository() {
        return new NacosRuleRepositoryAdapter<>(AuthorityRuleEntity.class);
    }

    @Bean
    public NacosRuleRepositoryAdapter<GatewayFlowRuleEntity> gatewayFlowRuleNacosRepository() {
        return new NacosRuleRepositoryAdapter<>(GatewayFlowRuleEntity.class);
    }

    @Bean
    public NacosRuleRepositoryAdapter<ApiDefinitionEntity> apiDefinitionNacosRepository() {
        return new NacosRuleRepositoryAdapter<>(ApiDefinitionEntity.class);
    }
}
