package com.coredeux.spring.boot.autoconfigure.redis;

import io.lettuce.core.api.StatefulRedisConnection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import com.coredeux.core.redis.service.impl.DefaultCoredeuxRedisDataAccessService;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
@ConditionalOnClass({ LettuceConnectionFactory.class, StatefulRedisConnection.class })
@ConditionalOnBean(LettuceConnectionFactory.class)
public class CoredeuxRedisAutoConfiguration {

    @Bean(name = "coredeuxRedisConnection")
    @ConditionalOnMissingBean(name = "coredeuxRedisConnection")
    public StatefulRedisConnection<String, String> coredeuxRedisConnection(LettuceConnectionFactory connectionFactory) {
        Object nativeConnection = connectionFactory.getConnection().getNativeConnection();
        if (nativeConnection instanceof StatefulRedisConnection<?, ?> lettuceConnection) {
            @SuppressWarnings("unchecked")
            StatefulRedisConnection<String, String> typedConnection = (StatefulRedisConnection<String, String>) lettuceConnection;
            return typedConnection;
        }
        throw new IllegalStateException("LettuceConnectionFactory did not provide a StatefulRedisConnection");
    }

    @Bean(name = "defaultCoredeuxRedisDataAccessService")
    @ConditionalOnMissingBean(name = "defaultCoredeuxRedisDataAccessService")
    public DefaultCoredeuxRedisDataAccessService defaultCoredeuxRedisDataAccessService(
            StatefulRedisConnection<String, String> coredeuxRedisConnection,
            @Value("${coredeux.redis.default-key-prefix:}") String keyPrefix) {
        return new DefaultCoredeuxRedisDataAccessService(coredeuxRedisConnection, keyPrefix);
    }
}
