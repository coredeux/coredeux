package com.coredeux.spring.boot.autoconfigure.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.StringUtils;

import com.coredeux.core.redis.service.impl.DefaultCoredeuxRedisDataAccessService;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
@ConditionalOnClass({ LettuceConnectionFactory.class, RedisClient.class, StatefulRedisConnection.class })
@ConditionalOnBean(LettuceConnectionFactory.class)
public class CoredeuxRedisAutoConfiguration {

    @Bean(name = "coredeuxRedisClient", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "coredeuxRedisClient")
    public RedisClient coredeuxRedisClient(LettuceConnectionFactory connectionFactory) {
        RedisURI redisUri = buildRedisUri(connectionFactory.getStandaloneConfiguration(),
                connectionFactory.getClientConfiguration());
        return RedisClient.create(redisUri);
    }

    @Bean(name = "coredeuxRedisConnection", destroyMethod = "close")
    @ConditionalOnMissingBean(name = "coredeuxRedisConnection")
    public StatefulRedisConnection<String, String> coredeuxRedisConnection(RedisClient coredeuxRedisClient) {
        return coredeuxRedisClient.connect(StringCodec.UTF8);
    }

    @Bean(name = "defaultCoredeuxRedisDataAccessService")
    @ConditionalOnMissingBean(name = "defaultCoredeuxRedisDataAccessService")
    public DefaultCoredeuxRedisDataAccessService defaultCoredeuxRedisDataAccessService(
            StatefulRedisConnection<String, String> coredeuxRedisConnection,
            @Value("${coredeux.redis.default-key-prefix:}") String keyPrefix) {
        return new DefaultCoredeuxRedisDataAccessService(coredeuxRedisConnection, keyPrefix);
    }

    private RedisURI buildRedisUri(RedisStandaloneConfiguration standaloneConfiguration,
            LettuceClientConfiguration clientConfiguration) {

        RedisURI.Builder builder = RedisURI.Builder.redis(standaloneConfiguration.getHostName(),
                standaloneConfiguration.getPort());

        builder.withDatabase(standaloneConfiguration.getDatabase());
        builder.withSsl(clientConfiguration.isUseSsl());
        builder.withVerifyPeer(clientConfiguration.isVerifyPeer());
        builder.withStartTls(clientConfiguration.isStartTls());
        builder.withTimeout(clientConfiguration.getCommandTimeout());

        String username = standaloneConfiguration.getUsername();
        if (StringUtils.hasText(username)) {
            standaloneConfiguration.getPassword().toOptional()
                    .ifPresent(password -> builder.withAuthentication(username, new String(password)));
        } else {
            standaloneConfiguration.getPassword().toOptional().ifPresent(builder::withPassword);
        }

        return builder.build();
    }
}
