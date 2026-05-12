package com.coredeux.spring.boot.autoconfigure;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.registry.CoredeuxComponentRegistry;

public class SpringCoredeuxComponentRegistry implements CoredeuxComponentRegistry {

    private final ApplicationContext applicationContext;

    public SpringCoredeuxComponentRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public <T> T getComponent(String name, Class<T> type) {
        try {
            return applicationContext.getBean(name, type);
        } catch (NoSuchBeanDefinitionException exception) {
            throw new CoredeuxStrategyException("Unable to resolve Coredeux component: " + name, exception);
        }
    }
}
