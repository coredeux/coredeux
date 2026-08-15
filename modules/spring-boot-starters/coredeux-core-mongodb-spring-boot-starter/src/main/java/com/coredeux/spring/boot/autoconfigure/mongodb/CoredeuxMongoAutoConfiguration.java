package com.coredeux.spring.boot.autoconfigure.mongodb;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.coredeux.core.mongodb.service.impl.DefaultCoredeuxMongoDataAccessService;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration")
@ConditionalOnClass(MongoTemplate.class)
@ConditionalOnBean(MongoTemplate.class)
@ConditionalOnProperty(prefix = "coredeux.mongodb", name = "enabled", havingValue = "true")
public class CoredeuxMongoAutoConfiguration {

    @Bean(name = "defaultCoredeuxMongoDataAccessService")
    @ConditionalOnMissingBean(name = "defaultCoredeuxMongoDataAccessService")
    public DefaultCoredeuxMongoDataAccessService defaultCoredeuxMongoDataAccessService(MongoTemplate mongoTemplate) {
        return new DefaultCoredeuxMongoDataAccessService(mongoTemplate);
    }
}
