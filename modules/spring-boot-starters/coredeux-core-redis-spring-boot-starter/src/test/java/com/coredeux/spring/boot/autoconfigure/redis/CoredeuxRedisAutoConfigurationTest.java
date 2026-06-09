package com.coredeux.spring.boot.autoconfigure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import com.coredeux.core.redis.service.impl.DefaultCoredeuxRedisDataAccessService;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

class CoredeuxRedisAutoConfigurationTest {

    @Test
    void buildsRedisUriWithUsernameAndPassword() throws Exception {
        CoredeuxRedisAutoConfiguration configuration = new CoredeuxRedisAutoConfiguration();
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration("localhost", 6380);
        standalone.setDatabase(2);
        standalone.setUsername("demo");
        standalone.setPassword(RedisPassword.of("secret"));
        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(3))
                .useSsl()
                .build();

        RedisURI redisURI = invokeBuildRedisUri(configuration, standalone, clientConfiguration);

        assertEquals("localhost", redisURI.getHost());
        assertEquals(6380, redisURI.getPort());
        assertEquals(2, redisURI.getDatabase());
        assertThat(redisURI.getUsername()).isEqualTo("demo");
        assertThat(redisURI.isSsl()).isTrue();
    }

    @Test
    void buildsRedisUriWithoutUsernameAndCreatesClientAndService() throws Exception {
        CoredeuxRedisAutoConfiguration configuration = new CoredeuxRedisAutoConfiguration();
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration("localhost", 6379);
        standalone.setDatabase(1);
        standalone.setPassword(RedisPassword.of("secret"));
        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder().build();

        RedisURI redisURI = invokeBuildRedisUri(configuration, standalone, clientConfiguration);
        assertEquals(1, redisURI.getDatabase());

        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(standalone, clientConfiguration);
        connectionFactory.afterPropertiesSet();
        RedisClient client = assertDoesNotThrow(() -> configuration.coredeuxRedisClient(connectionFactory));
        assertThat(client).isNotNull();
        client.shutdown();

        StatefulRedisConnection<String, String> connection = mock(StatefulRedisConnection.class);
        DefaultCoredeuxRedisDataAccessService service =
                configuration.defaultCoredeuxRedisDataAccessService(connection, " prefix ");
        assertThat(service).isNotNull();
    }

    private RedisURI invokeBuildRedisUri(CoredeuxRedisAutoConfiguration configuration,
            RedisStandaloneConfiguration standaloneConfiguration, LettuceClientConfiguration clientConfiguration)
            throws Exception {
        Method method = CoredeuxRedisAutoConfiguration.class.getDeclaredMethod("buildRedisUri",
                RedisStandaloneConfiguration.class, LettuceClientConfiguration.class);
        method.setAccessible(true);
        return (RedisURI) method.invoke(configuration, standaloneConfiguration, clientConfiguration);
    }
}
