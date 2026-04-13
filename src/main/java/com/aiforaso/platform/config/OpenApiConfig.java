package com.aiforaso.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI asoPlatformOpenApi() {
        return new OpenAPI().info(new Info()
                .title("AI for ASO Drug R&D Platform")
                .description("Prototype APIs for literature management, indicator extraction, RAG analysis and knowledge graph views.")
                .version("v0.1.0")
                .contact(new Contact().name("AI for ASO Platform")));
    }
}
