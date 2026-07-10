package com.smartelderly.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String scheme = "bearer-jwt";
        return new OpenAPI()
                .addServersItem(new Server().url("/api").description("与 server.servlet.context-path 一致"))
                .info(new Info().title("SECP API").description("SOS 等接口；请先点击 Authorize 填入 Bearer Token（与登录同事对齐密钥与 claims）。"))
                .components(new Components()
                        .addSecuritySchemes(
                                scheme,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Authorization: Bearer <token>，载荷需含 sub(用户id)、role(elder|child)")))
                .addSecurityItem(new SecurityRequirement().addList(scheme));
    }
}
