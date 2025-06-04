package com.drumtong.backend.common.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${jwt.access.header}")
    private String accessTokenHeader;

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme accessTokenScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name(accessTokenHeader);

        SecurityRequirement accessTokenRequirement = new SecurityRequirement()
                .addList(accessTokenHeader);

        // 서버(서버 URL) 정보
        Server server = new Server();
        server.setUrl("https://api.ddalkkug.kro.kr");

        return new OpenAPI()
                .info(new Info()
                        .title("드럼통")
                        .description("드럼통 REST API Document")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(accessTokenHeader, accessTokenScheme))
                .addServersItem(server)
                .addSecurityItem(accessTokenRequirement);
    }
}
