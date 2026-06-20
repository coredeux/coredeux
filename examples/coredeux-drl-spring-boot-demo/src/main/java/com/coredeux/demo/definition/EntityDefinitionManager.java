package com.coredeux.demo.definition;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.loader.EntityDefinitionLoader;
import com.coredeux.core.registry.EntityDefinitionRegistries;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.demo.config.DemoEntityDefinitionProperties;
import com.coredeux.demo.domain.EntityDefinitionRegistryRecord;

@Component
public class EntityDefinitionManager {

    private static final Logger log = LoggerFactory.getLogger(EntityDefinitionManager.class);

	private final EntityDefinitionRegistryRecordRepository repository;
    private final DemoEntityDefinitionProperties properties;
    private final ResourceLoader resourceLoader;
    private final EntityDefinitionLoader definitionLoader;
    private final StringRedisTemplate redisTemplate;
    private final AtomicReference<EntityDefinitionRegistry> snapshot = new AtomicReference<>();

    public EntityDefinitionManager(EntityDefinitionRegistryRecordRepository repository,
			DemoEntityDefinitionProperties properties, ResourceLoader resourceLoader,
			EntityDefinitionLoader definitionLoader, StringRedisTemplate redisTemplate) {
		this.repository = repository;
		this.properties = properties;
		this.resourceLoader = resourceLoader;
		this.definitionLoader = definitionLoader;
		this.redisTemplate = redisTemplate;
	}
    

    @Transactional
    public Optional<EntityDefinitionRegistryRecord> updateEntityDefinitionFromFile() {
        return loadBootstrapYaml().map(yaml -> updateDefinition(yaml, properties.getBootstrapLocation()));
    }

    @Transactional(readOnly = true)
    public Optional<EntityDefinitionRegistryRecord> findRecord() {
        return repository.findByCode(properties.getRegistryCode());
    }

    public boolean isCached() {
        return Boolean.TRUE.equals(redisTemplate.hasKey(properties.getRedisKey()));
    }

    public String getCachedYML() {
    	 return redisTemplate.opsForValue().get(properties.getRedisKey());
    }
    
    public void refreshCache() {
        String yaml = findRecord()
                .map(EntityDefinitionRegistryRecord::getYaml)
                .orElse(null);
        if (yaml == null) {
            initializeEmptyRegistryIfNeeded();
            return;
        }
        refreshCache(yaml);
    }

    public void purgeCache() {
        redisTemplate.delete(properties.getRedisKey());
        snapshot.set(null);
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
        purgeCache();
        refreshCache(saved.getYaml());
        return saved;
    }

    public EntityDefinitionRegistry currentRegistry() {
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
                    Optional<String> bootstrapYaml = loadBootstrapYaml();
                    if (bootstrapYaml.isEmpty()) {
                        return initializeEmptyRegistryIfNeeded();
                    }
                    yaml = bootstrapYaml.get();
                    redisTemplate.opsForValue().set(properties.getRedisKey(), yaml);
                } else {
                    redisTemplate.opsForValue().set(properties.getRedisKey(), yaml);
                }
            }
            if (yaml == null || yaml.isBlank()) {
                return initializeEmptyRegistryIfNeeded();
            }
            current = registryFromYaml(yaml);
            snapshot.set(current);
            return current;
        }
    }
    
    private void refreshCache(String yaml) {
        redisTemplate.opsForValue().set(properties.getRedisKey(), yaml);
        snapshot.set(registryFromYaml(yaml));
    }

    private EntityDefinitionRegistry registryFromYaml(String yaml) {
        try (InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))) {
            return EntityDefinitionRegistries.fromYaml(inputStream, definitionLoader);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to load entity definitions from YAML payload", exception);
        }
    }

    private Optional<String> loadBootstrapYaml() {
        Resource resource = resourceLoader.getResource(properties.getBootstrapLocation());
        if (!resource.exists()) {
            log.info("Entity definition bootstrap resource not found, using an empty registry instead: {}",
                    properties.getBootstrapLocation());
            return Optional.empty();
        }
        try (InputStream inputStream = resource.getInputStream()) {
            String yaml = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            definitionLoader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
            return Optional.of(yaml);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Unable to read entity definition bootstrap resource: " + properties.getBootstrapLocation(),
                    exception);
        }
    }

    private EntityDefinitionRegistry initializeEmptyRegistryIfNeeded() {
        EntityDefinitionRegistry current = snapshot.get();
        if (current != null) {
            return current;
        }
        synchronized (snapshot) {
            current = snapshot.get();
            if (current != null) {
                return current;
            }
            redisTemplate.delete(properties.getRedisKey());
            current = EntityDefinitionRegistries.empty();
            snapshot.set(current);
            return current;
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
