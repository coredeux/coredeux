package com.coredeux.demo.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.loader.EntityDefinitionLoader;
import com.coredeux.core.loader.YamlEntityDefinitionLoader;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.demo.config.DemoEntityDefinitionProperties;
import com.coredeux.demo.domain.EntityDefinitionRegistryRecord;

@ExtendWith(MockitoExtension.class)
class EntityDefinitionManagerTest {

    private static final String REGISTRY_CODE = "coredeux-demo";
    private static final String REDIS_KEY = "coredeux:demo:entity-definitions";
    private static final String BOOTSTRAP_LOCATION = "classpath:test-coredeux-entities.yml";
    private static final String YAML = """
            coredeux:
              entities:
                - full-class-name: com.coredeux.demo.domain.Customer
                  name: customer
                  identifier: pk
                  storage:
                    data-access-service: customerDataAccess.drl
            """;

    @Mock
    private EntityDefinitionRegistryRecordRepository repository;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private DemoEntityDefinitionProperties properties;
    private EntityDefinitionLoader definitionLoader;
    private EntityDefinitionManager manager;

    @BeforeEach
    void setUp() {
        properties = new DemoEntityDefinitionProperties();
        properties.setRegistryCode(REGISTRY_CODE);
        properties.setBootstrapLocation(BOOTSTRAP_LOCATION);
        properties.setRedisKey(REDIS_KEY);
        properties.setBootstrapEnabled(true);
        definitionLoader = new YamlEntityDefinitionLoader();
        manager = new EntityDefinitionManager(repository, properties, resourceLoader, definitionLoader, redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void resolvesRecordCacheAndRegistryState() {
        EntityDefinitionRegistryRecord record = record(REGISTRY_CODE, BOOTSTRAP_LOCATION, YAML);
        when(repository.findByCode(REGISTRY_CODE)).thenReturn(Optional.of(record));
        when(redisTemplate.hasKey(REDIS_KEY)).thenReturn(true);
        when(valueOperations.get(REDIS_KEY)).thenReturn(YAML);

        assertEquals(Optional.of(record), manager.findRecord());
        assertEquals(true, manager.isCached());
        assertEquals(YAML, manager.getCachedYML());

        EntityDefinitionRegistry first = manager.currentRegistry();
        EntityDefinitionRegistry second = manager.currentRegistry();

        assertSame(first, second);
        assertEquals(1, first.getAll().size());
    }

    @Test
    void updatesBootstrapDefinitionAndRefreshesCache() {
        Resource resource = new ByteArrayResource(YAML.getBytes(StandardCharsets.UTF_8));
        when(resourceLoader.getResource(BOOTSTRAP_LOCATION)).thenReturn(resource);
        when(repository.findByCode(REGISTRY_CODE)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EntityDefinitionRegistryRecord saved = manager.updateEntityDefinitionFromFile();

        assertEquals(REGISTRY_CODE, saved.getCode());
        assertEquals(BOOTSTRAP_LOCATION, saved.getSourceLocation());
        assertEquals(YAML, saved.getYaml());
        assertNotNull(saved.getUpdatedAt());
        verify(redisTemplate).delete(REDIS_KEY);
        verify(valueOperations).set(REDIS_KEY, YAML);
    }

    @Test
    void refreshCacheFallsBackToBootstrapWhenNoStoredRecordExists() {
        EntityDefinitionRegistryRecord saved = record(REGISTRY_CODE, BOOTSTRAP_LOCATION, YAML);
        EntityDefinitionManager spy = org.mockito.Mockito.spy(manager);
        doReturn(Optional.empty()).when(spy).findRecord();
        doReturn(saved).when(spy).updateEntityDefinitionFromFile();

        spy.refreshCache();

        verify(spy).updateEntityDefinitionFromFile();
    }

    @Test
    void purgeCacheClearsRedisAndInMemorySnapshot() {
        manager.purgeCache();

        verify(redisTemplate).delete(REDIS_KEY);
        @SuppressWarnings("unchecked")
        AtomicReference<?> snapshot = (AtomicReference<?>) ReflectionTestUtils.getField(manager, "snapshot");
        assertNotNull(snapshot);
        assertNull(snapshot.get());
    }

    @Test
    void updateDefinitionNormalizesSourceLocationAndRejectsBlankYaml() {
        when(repository.findByCode(REGISTRY_CODE)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EntityDefinitionRegistryRecord saved = manager.updateDefinition(YAML, "  postman  ");
        assertEquals("postman", saved.getSourceLocation());

        assertThrows(CoredeuxValidationException.class, () -> manager.updateDefinition(" ", null));
    }

    private EntityDefinitionRegistryRecord record(String code, String sourceLocation, String yaml) {
        EntityDefinitionRegistryRecord record = new EntityDefinitionRegistryRecord();
        record.setCode(code);
        record.setSourceLocation(sourceLocation);
        record.setYaml(yaml);
        return record;
    }
}
