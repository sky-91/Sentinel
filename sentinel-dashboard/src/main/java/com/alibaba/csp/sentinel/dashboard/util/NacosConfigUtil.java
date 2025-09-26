package com.alibaba.csp.sentinel.dashboard.util;

public final class NacosConfigUtil {

    public static final String FLOW_DATA_ID_POSTFIX = "-flow-rules";
    public static final String DEGRADE_DATA_ID_POSTFIX = "-degrade-rules";
    public static final String PARAM_FLOW_DATA_ID_POSTFIX = "-param-rules";
    public static final String SYSTEM_DATA_ID_POSTFIX = "-system-rules";
    public static final String AUTHORITY_DATA_ID_POSTFIX = "-authority-rules";
    public static final String GATEWAY_FLOW_DATA_ID_POSTFIX = "-gw-flow-rules";
    public static final String API_DEFINITION_DATA_ID_POSTFIX = "-api-definition";

    public static String getDataId(String appName, String postfix) {
        return appName + postfix;
    }
}
