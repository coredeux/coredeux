package com.coredeux.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.coredeux.core.jpa.service.impl.PostgresCoredeuxJpaDataAccessService;

@Configuration(proxyBeanMethods = false)
public class DemoJpaConfiguration {

    @Bean(name = "postgresCoredeuxJpaDataAccessService")
    public PostgresCoredeuxJpaDataAccessService postgresCoredeuxJpaDataAccessService() {
        return new PostgresCoredeuxJpaDataAccessService();
    }
}
