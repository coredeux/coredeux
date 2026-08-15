package com.coredeux.spring.boot.autoconfigure.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import com.coredeux.core.jpa.service.impl.DefaultCoredeuxJpaDataAccessService;
import com.coredeux.core.jpa.service.impl.PostgresCoredeuxJpaDataAccessService;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration")
@ConditionalOnClass({ EntityManager.class, EntityManagerFactory.class })
@ConditionalOnBean(EntityManagerFactory.class)
@ConditionalOnProperty(prefix = "coredeux.jpa", name = "enabled", havingValue = "true")
public class CoredeuxJpaAutoConfiguration {

    @Bean(name = "defaultCoredeuxJpaDataAccessService")
    @ConditionalOnMissingBean(name = "defaultCoredeuxJpaDataAccessService")
    public DefaultCoredeuxJpaDataAccessService defaultCoredeuxJpaDataAccessService() {
        return new DefaultCoredeuxJpaDataAccessService();
    }

    @Bean(name = "postgresCoredeuxJpaDataAccessService")
    @ConditionalOnMissingBean(name = "postgresCoredeuxJpaDataAccessService")
    public PostgresCoredeuxJpaDataAccessService postgresCoredeuxJpaDataAccessService() {
        return new PostgresCoredeuxJpaDataAccessService();
    }
}
