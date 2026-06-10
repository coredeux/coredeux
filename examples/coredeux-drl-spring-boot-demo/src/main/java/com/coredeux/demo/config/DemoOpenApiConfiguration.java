package com.coredeux.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DemoOpenApiConfiguration {

    @Bean
    public OpenAPI demoOpenAPI() {
        return new OpenAPI().info(new Info().title("Coredeux DRL Spring Boot Demo")
                .description("Small DRL proof-of-concept demo"));
    }
}
