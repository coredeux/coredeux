package com.coredeux.demo.definition;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.loader.EntityDefinitionLoader;
import com.coredeux.core.registry.EntityDefinitionRegistries;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.demo.config.DemoEntityDefinitionProperties;
import com.coredeux.demo.domain.EntityDefinitionRegistryRecord;

/**
 * Database-backed entity definition registry with Redis as a refreshable cache.
 */
@Service
@org.springframework.context.annotation.Primary
public class DatabaseBackedEntityDefinitionRegistry implements EntityDefinitionRegistry {

    private final EntityDefinitionRegistryRecordRepository repository;
    private final DemoEntityDefinitionProperties properties;
    private final ResourceLoader resourceLoader;
    private final EntityDefinitionLoader definitionLoader;
    private final StringRedisTemplate redisTemplate;
    private final AtomicReference<EntityDefinitionRegistry> snapshot = new AtomicReference<>();

    public DatabaseBackedEntityDefinitionRegistry(EntityDefinitionRegistryRecordRepository repository,
            DemoEntityDefinitionProperties properties, ResourceLoader resourceLoader,
            EntityDefinitionLoader definitionLoader, StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.definitionLoader = definitionLoader;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<CoredeuxEntityDefinition> findByFullClassName(String fullClassName) {
        return currentRegistry().findByFullClassName(fullClassName);
    }

    @Override
    public Collection<CoredeuxEntityDefinition> getAll() {
        return currentRegistry().getAll();
    }

    @Transactional
    public EntityDefinitionRegistryRecord bootstrapFromFile() {
        return updateDefinition(loadBootstrapYaml(), properties.getBootstrapLocation());
    }

    @Transactional(readOnly = true)
    public Optional<EntityDefinitionRegistryRecord> findRecord() {
        return repository.findByCode(properties.getRegistryCode());
    }

    public boolean isCached() {
        return Boolean.TRUE.equals(redisTemplate.hasKey(properties.getRedisKey()));
    }

    public void refreshCache() {
        String yaml = findRecord()
                .map(EntityDefinitionRegistryRecord::getYaml)
                .orElse(null);
        if (yaml == null) {
            bootstrapFromFile();
            return;
        }
        refreshCache(yaml);
    }

    public void purgeCache() {
        redisTemplate.delete(properties.getRedisKey());
        snapshot.set(null);
    }

    public EntityDefinitionRegistryRecord updateFromFile() {
        return bootstrapFromFile();
    }

    @Transactional
    public EntityDefinitionRegistryRecord updateDefinition(String yaml, String sourceLocation) {
        String normalizedYaml = requireYaml(yaml);
        EntityDefinitionRegistryRecord record = repository.findByCode(properties.getRegistryCode())
                .orElseGet(EntityDefinitionRegistryRecord::new);
        record.setCode(properties.getRegistryCode());
        record.setSourceLocation(normalizeSourceLocation(sourceLocation));
        record.setYaml(normalizedYaml);
        record.setUpdatedAt(Instant.now());
        EntityDefinitionRegistryRecord saved = repository.save(record);
        refreshCache(saved.getYaml());
        return saved;
    }

    private void refreshCache(String yaml) {
        redisTemplate.opsForValue().set(properties.getRedisKey(), yaml);
        snapshot.set(registryFromYaml(yaml));
    }

    private EntityDefinitionRegistry currentRegistry() {
        EntityDefinitionRegistry current = snapshot.get();
        if (current != null) {
            return current;
        }
        synchronized (snapshot) {
            current = snapshot.get();
            if (current != null) {
                return current;
            }
            String yaml = redisTemplate.opsForValue().get(properties.getRedisKey());
            if (yaml == null) {
                yaml = findRecord().map(EntityDefinitionRegistryRecord::getYaml).orElse(null);
                if (yaml == null) {
                    bootstrapFromFile();
                    return currentRegistry();
                }
                redisTemplate.opsForValue().set(properties.getRedisKey(), yaml);
            }
            if (yaml == null || yaml.isBlank()) {
                throw new CoredeuxValidationException("No entity definition registry is available");
            }
            current = registryFromYaml(yaml);
            snapshot.set(current);
            return current;
        }
    }

    private EntityDefinitionRegistry registryFromYaml(String yaml) {
        try (InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))) {
            return EntityDefinitionRegistries.fromYaml(inputStream, definitionLoader);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to load entity definitions from YAML payload", exception);
        }
    }

    private String loadBootstrapYaml() {
        Resource resource = resourceLoader.getResource(properties.getBootstrapLocation());
        if (!resource.exists()) {
            throw new CoredeuxValidationException(
                    "Entity definition bootstrap resource not found: " + properties.getBootstrapLocation());
        }
        try (InputStream inputStream = resource.getInputStream()) {
            String yaml = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            definitionLoader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
            return yaml;
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Unable to read entity definition bootstrap resource: " + properties.getBootstrapLocation(),
                    exception);
        }
    }

    private String requireYaml(String yaml) {
        if (yaml == null || yaml.isBlank()) {
            throw new CoredeuxValidationException("Entity definition YAML must not be blank");
        }
        definitionLoader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        return yaml;
    }

    private String normalizeSourceLocation(String sourceLocation) {
        if (sourceLocation == null || sourceLocation.isBlank()) {
            return properties.getBootstrapLocation();
        }
        return sourceLocation.trim();
    }
}
