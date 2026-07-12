package com.smartelderly.api.control;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "robot.gateway")
public class RobotGatewayProperties {

    private String baseUrl = "http://127.0.0.1:7000";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
