package com.fashionsystem.fashion_system.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mô tả OpenAPI cho Swagger UI ({@code /swagger-ui.html}, bật bằng API_DOCS_ENABLED=true).
 * Nút Authorize nhận JWT từ {@code /api/auth/login/employee} hoặc {@code /api/auth/login/customer}.
 */
@Configuration
public class OpenApiConfig {
    private static final String BEARER = "bearer-jwt";

    @Bean
    OpenAPI fashionSystemOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lunaria Boutique API")
                        .version("v1")
                        .description("""
                                REST API của hệ thống Lunaria Boutique.
                                Đăng nhập bằng POST /api/auth/login/employee (nhân viên) hoặc
                                /api/auth/login/customer (khách), rồi dán `token` vào nút Authorize."""))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    @Bean
    GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder().group("1-admin")
                .pathsToMatch("/api/**")
                .pathsToExclude("/api/storefront/**", "/api/customers/me/**", "/api/auth/**", "/api/webhooks/**")
                .build();
    }

    @Bean
    GroupedOpenApi storefrontApi() {
        return GroupedOpenApi.builder().group("2-storefront")
                .pathsToMatch("/api/storefront/**", "/api/customers/me/**")
                .build();
    }

    @Bean
    GroupedOpenApi authApi() {
        return GroupedOpenApi.builder().group("3-auth-webhooks")
                .pathsToMatch("/api/auth/**", "/api/webhooks/**")
                .build();
    }
}
