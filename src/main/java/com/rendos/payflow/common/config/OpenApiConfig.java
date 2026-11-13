package com.rendos.payflow.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI payflowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayFlow Financial API")
                        .description("Enterprise Payment & Transaction Management System built with Spring Boot 3 & PostgreSQL")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Ros Rendo")
                                .email("contact@payflow.local"))
                        .license(new License().name("Apache 2.0")));
    }
}

