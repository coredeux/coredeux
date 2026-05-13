package com.coredeux.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class DemoOpenApiConfiguration {

    @Bean
    OpenAPI coredeuxDemoOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Coredeux Demo API")
                .description("Interactive CRUD API for the Coredeux demo application. "
                        + "Entity resolution uses fully qualified class names in the path.")
                .version("0.1.0-SNAPSHOT")
                .contact(new Contact()
                        .name("Coredeux")
                        .url("https://github.com/coredeux/coredeux")));
    }
}
