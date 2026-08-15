package com.coredeux.spring.boot.autoconfigure.jdbc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.coredeux.core.jdbc.service.impl.DefaultCoredeuxJdbcDataAccessService;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration")
@ConditionalOnClass(NamedParameterJdbcTemplate.class)
@ConditionalOnBean(NamedParameterJdbcTemplate.class)
@ConditionalOnProperty(prefix = "coredeux.jdbc", name = "enabled", havingValue = "true")
public class CoredeuxJdbcAutoConfiguration {

    @Bean(name = "defaultCoredeuxJdbcDataAccessService")
    @ConditionalOnMissingBean(name = "defaultCoredeuxJdbcDataAccessService")
    public DefaultCoredeuxJdbcDataAccessService defaultCoredeuxJdbcDataAccessService(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            @Value("${coredeux.jdbc.default-schema:}") String defaultSchema) {
        return new DefaultCoredeuxJdbcDataAccessService(namedParameterJdbcTemplate, defaultSchema);
    }
}
