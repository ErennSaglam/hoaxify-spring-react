package com.hoaxify.ws.configuration;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Swagger UI: http://localhost:8080/swagger-ui.html
 * Login olduktan sonra sağ üstteki "Authorize" butonuna token değerini yapıştırabilirsin.
 * (Swagger UI'dan /api/v1/auth çağırırsan tarayıcı hoax-token cookie'sini de saklar.)
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "Hoaxify API", version = "v1"),
		security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
public class OpenApiConfiguration {

}
