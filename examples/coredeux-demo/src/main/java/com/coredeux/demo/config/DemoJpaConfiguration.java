package com.coredeux.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.coredeux.core.jpa.service.impl.PostgresCoredeuxJpaDataAccessService;

import jakarta.persistence.EntityManagerFactory;

@Configuration(proxyBeanMethods = false)
public class DemoJpaConfiguration {

    @Bean(name = "postgresCoredeuxJpaDataAccessService", destroyMethod = "close")
    public PostgresCoredeuxJpaDataAccessService postgresCoredeuxJpaDataAccessService(
            EntityManagerFactory entityManagerFactory) {
        return new PostgresCoredeuxJpaDataAccessService(entityManagerFactory);
    }
}
