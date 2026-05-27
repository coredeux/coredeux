# Property Resolution Order

<!-- docs-nav-start -->
[Previous: Core Redis Reference](/18-core-redis-reference) | [Documentation Home](/) | [Next: Coredeux Import](/coredeux-import-overview)
<!-- docs-nav-end -->

This page explains how Coredeux resolves configuration in practice.

The short version is:

1. Spring Boot application properties win first
2. Coredeux bundled properties come next
3. Coredeux hard defaults come last

That is the same rule for both entity-definition location and import-related
settings.

## The Two Property Sources

### Spring Boot Configuration

In a Spring Boot application, put settings in:

- `src/main/resources/application.yml`
- or `src/main/resources/application.properties`

The Spring Boot starters read from the Spring `Environment` first. That means
these values win when they are present.

### Coredeux Bundled Configuration

Coredeux core can also load:

- `src/main/resources/META-INF/coredeux.yml`

That file is loaded into `CoredeuxProperties` and acts as the bundled fallback
for native applications or for Spring apps that want a second config source.

## Resolution Order

### Entity Definition Location

For entity definitions, the Spring Boot core starter resolves:

1. `coredeux.entities.config-location` from the Spring `Environment`
2. `coredeux.entities.config-location` from `CoredeuxProperties`
3. `classpath:coredeux-entities.yml`

That means this Spring Boot setting works as the explicit override:

```yaml
coredeux:
  entities:
    config-location: classpath:coredeux-entities.yml
```

### Import Default Parser

For import settings, the Spring Boot import starter resolves:

1. `coredeux.import.default-parser` from the Spring `Environment`
2. `coredeux.import.default-parser` from `CoredeuxProperties`
3. `text`

That means this Spring Boot setting works as the explicit override:

```yaml
coredeux:
  import:
    default-parser: text
```

## Why This Exists

This split keeps Spring Boot apps simple:

- application owners can keep using normal Spring Boot config files
- Coredeux still has a bundled fallback for native or embedded use
- the runtime stays predictable because the override order is fixed

## Mental Model

Think of configuration in this order:

```mermaid
flowchart LR
    A["application.yml / application.properties"] --> B["Spring starter reads Environment"]
    B --> C["CoredeuxProperties fallback"]
    C --> D["coredeux-core loads the resolved YAML or property value"]
```

That is the only thing you need to remember when you are wiring Coredeux
properties in a Spring Boot host.

<!-- docs-nav-start -->
[Previous: Core Redis Reference](/18-core-redis-reference) | [Documentation Home](/) | [Next: Coredeux Import](/coredeux-import-overview)
<!-- docs-nav-end -->
