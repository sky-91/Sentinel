package com.alibaba.csp.sentinel.dashboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NacosConfig {

    @Value("${nacos.address:localhost:8848}")
    private String serverAddr;

    @Value("${nacos.namespace:}")
    private String namespace;

    @Value("${nacos.group.id:SENTINEL_CLUSTER_GROUP}")
    private String groupId;

    // getter
    public String getServerAddr() { return serverAddr; }

    public String getNamespace() { return namespace; }

    public String getGroupId() {
        return groupId;
    }
}