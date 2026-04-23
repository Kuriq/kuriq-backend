package com.example.kuriq.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Swagger에 “bearerAuth”라는 JWT 인증 방식을 등록하는 클래스 */

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Kuriq API",
                version = "v1",
                description = "Kuriq 백엔드 API 문서"
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)

public class SwaggerConfig {
}
