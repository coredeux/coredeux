package com.coredeux.spring.boot.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.coredeux.core.resolver.context.CoredeuxRequestContextResolver;

@AutoConfiguration(after = CoredeuxWebAutoConfiguration.class)
public class CoredeuxRequestContextAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    CoredeuxRequestContextResolver coredeuxRequestContextResolver() {
        return () -> null;
    }
}
