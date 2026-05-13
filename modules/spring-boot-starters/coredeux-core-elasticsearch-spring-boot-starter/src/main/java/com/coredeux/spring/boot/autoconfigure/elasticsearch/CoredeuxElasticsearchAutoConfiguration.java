package com.coredeux.spring.boot.autoconfigure.elasticsearch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import com.coredeux.core.elasticsearch.service.impl.DefaultCoredeuxElasticsearchDataAccessService;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration")
@ConditionalOnClass(ElasticsearchOperations.class)
@ConditionalOnBean(ElasticsearchOperations.class)
public class CoredeuxElasticsearchAutoConfiguration {

    @Bean(name = "defaultCoredeuxElasticsearchDataAccessService")
    @ConditionalOnMissingBean(name = "defaultCoredeuxElasticsearchDataAccessService")
    public DefaultCoredeuxElasticsearchDataAccessService defaultCoredeuxElasticsearchDataAccessService(
            ElasticsearchOperations elasticsearchOperations,
            @Value("${coredeux.elasticsearch.default-index-prefix:}") String defaultIndexPrefix) {
        return new DefaultCoredeuxElasticsearchDataAccessService(elasticsearchOperations, defaultIndexPrefix);
    }
}
