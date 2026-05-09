# Adding A Core Module

<!-- docs-nav-start -->
[Previous: coredeux-core Reference](reference.md) | [Documentation Home](../../README.md) | [Tutorial Order](../../SUMMARY.md) | [Next: coredeux-core-jpa Reference](../core-jpa/reference.md)
<!-- docs-nav-end -->

This guide describes how to add a new entity-level module to `coredeux-core`.

Use this when the framework needs a new configurable capability that can be enabled per entity through YAML, such as workflow, import/export, enrichment, security policy checks, notification dispatch, or another lifecycle-aware behavior.

## Module Model

Coredeux modules are configured on an entity definition under `modules`.

Each configured module is represented by `CoredeuxModuleDefinition` and dispatched by a Spring bean that implements `CoredeuxEntityModuleHandler`.

The normal flow is:

1. Coredeux resolves the entity definition.
2. The strategy layer walks the enabled module definitions for that entity.
3. `AbstractCoredeuxStrategy` finds the matching `CoredeuxEntityModuleHandler` by `getModuleName()`.
4. The module handler resolves any configured handler beans listed in YAML.
5. The module handler executes the feature for the current phase and operation context.

## 1. Choose The Module Name

Pick a stable lowercase module name. This is the value users will put in YAML.

Examples already in core:

- `validators`
- `hooks`
- `audit`

Prefer a plural name when the module executes one or more user-provided handler beans, such as `workflows`, `policies`, or `notifications`.

The module name must be unique. Core rejects duplicate `CoredeuxEntityModuleHandler` beans with the same module name when the strategy is constructed.

## 2. Decide Whether The Module Needs A Public Contract

Add a dedicated interface when application code must implement feature-specific behavior.

Existing examples:

- `CoredeuxEntityValidator<T>` for the `validators` module
- `CoredeuxEntityHook<T>` for the `hooks` module
- `CoredeuxEntityAuditHandler<T>` for the `audit` module

Create the interface in a feature package under `modules/coredeux-core/src/main/java/com/coredeux/core`.

Example:

```java
package com.coredeux.core.workflow;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;

public interface CoredeuxEntityWorkflowHandler<T> {

    void execute(T entity, CoredeuxEntityDefinition definition, OperationContext context);
}
```

Keep the contract small. Pass `CoredeuxEntityDefinition` and `OperationContext` when implementors need entity metadata, request context, lifecycle operation, identifier, old value, or new value.

Do not add a dedicated public contract if the module only interprets static configuration and does not call user-provided beans.

## 3. Define The YAML Shape

Every module uses the same top-level shape:

```yaml
modules:
  - name: workflows
    enabled: true
    handlers:
      - customerApprovalWorkflow
    config:
      phases:
        - AFTER_SAVE
```

Use these fields consistently:

- `name`: the module name returned by `getModuleName()`
- `enabled`: optional flag, defaults to enabled when omitted
- `handlers`: Spring bean names for user-provided extension beans
- `config`: module-specific settings

Keep module-specific settings inside `config`; do not add new top-level entity fields unless the capability is truly part of the shared entity model.

## 4. Update Configuration Loading Only When Needed

`YamlEntityDefinitionLoader` already loads unknown module `config` values as immutable maps and lists.

Most modules do not need loader changes. Read their settings through:

```java
Map<String, Object> configMap = moduleDefinition.getConfigMap();
```

Only update `YamlEntityDefinitionLoader.convertModuleConfig(...)` when the module needs a typed config object or a special structured model, like the existing `attributes` handling.

If you add typed config conversion, also add loader tests for:

- missing required fields
- invalid shapes
- default behavior
- immutable or defensive-copy behavior where applicable

## 5. Implement The Module Handler

Create a handler in `com.coredeux.core.module.impl` and annotate it with `@Component`.

The handler must implement `CoredeuxEntityModuleHandler`.

Example:

```java
package com.coredeux.core.module.impl;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.workflow.CoredeuxEntityWorkflowHandler;

@Component
public class WorkflowsModuleHandler implements CoredeuxEntityModuleHandler {

    private final ApplicationContext applicationContext;

    public WorkflowsModuleHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public String getModuleName() {
        return "workflows";
    }

    @Override
    public <T> void execute(T entity, CoredeuxEntityDefinition definition,
            CoredeuxModuleDefinition moduleDefinition, String phase, OperationContext context) {
        if (!shouldRun(moduleDefinition, phase)) {
            return;
        }

        for (String handlerName : moduleDefinition.getHandlers()) {
            if (handlerName == null || handlerName.isBlank()) {
                continue;
            }

            CoredeuxEntityWorkflowHandler<T> handler = resolveHandler(handlerName.trim(), entity, definition);
            handler.execute(entity, definition, context);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> CoredeuxEntityWorkflowHandler<T> resolveHandler(String handlerName, T entity,
            CoredeuxEntityDefinition definition) {
        try {
            CoredeuxEntityWorkflowHandler<?> handler =
                    applicationContext.getBean(handlerName, CoredeuxEntityWorkflowHandler.class);
            validateSupportedType(handlerName, handler.getClass(), entity, definition);
            return (CoredeuxEntityWorkflowHandler<T>) handler;
        } catch (CoredeuxStrategyException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CoredeuxStrategyException("Unable to resolve workflow handler bean: " + handlerName
                    + " for class: " + definition.getFullClassName(), exception);
        }
    }

    private <T> void validateSupportedType(String beanName, Class<?> beanType, T entity,
            CoredeuxEntityDefinition definition) {
        Class<?> supportedType = ResolvableType.forClass(beanType)
                .as(CoredeuxEntityWorkflowHandler.class)
                .resolveGeneric(0);
        if (supportedType == null || entity == null || supportedType.isAssignableFrom(entity.getClass())) {
            return;
        }
        throw new CoredeuxStrategyException("Configured workflow handler bean '" + beanName
                + "' does not support entity type " + entity.getClass().getName()
                + " for class: " + definition.getFullClassName());
    }

    private boolean shouldRun(CoredeuxModuleDefinition moduleDefinition, String phase) {
        Map<String, Object> configMap = moduleDefinition.getConfigMap();
        Object configuredPhases = configMap.get("phases");
        if (configuredPhases == null) {
            return true;
        }
        if (!(configuredPhases instanceof List<?> phases) || CollectionUtils.isEmpty(phases)) {
            throw new CoredeuxStrategyException("Workflows module requires config.phases to be a non-empty list");
        }
        return phases.stream()
                .map(String::valueOf)
                .map(String::trim)
                .anyMatch(configuredPhase -> configuredPhase.equals(phase));
    }
}
```

The demo application's
[WorkflowsModuleHandler.java](../../../examples/coredeux-demo/src/main/java/com/coredeux/demo/workflow/WorkflowsModuleHandler.java)
uses this pattern. The strategy will call every enabled module for each
framework phase; the module handler decides whether the configured module is
relevant for that phase.

That distinction is important:

- `AbstractCoredeuxStrategy` selects enabled module definitions and dispatches
  them by module name.
- `DefaultCoredeuxStrategy` decides which phase is currently running, such as
  `before-save`, `after-save`, `before-update`, or `load`.
- the module handler owns module-specific execution rules, such as reading
  `config.phases` and returning immediately when the current phase is not
  configured.

Use the existing built-in handlers as the implementation style guide:

- `ValidatorsModuleHandler` for phase filtering and error aggregation
- `HooksModuleHandler` for phase-to-callback dispatch
- `AuditModuleHandler` for module-specific config validation

## 6. Respect Lifecycle Phases And Operations

Handlers receive both `phase` and `OperationContext`.

Use `phase` to decide when the module should execute. Use `context.getLifecycleContext().getOperation()` when behavior depends on the logical operation.

Canonical phase constants live in `CoredeuxHookPhases`.

Canonical operation constants live in `CoredeuxLifecycleOperations`.

Common patterns:

- validators run on `BEFORE_SAVE` and `BEFORE_UPDATE`
- hooks dispatch directly by phase
- audit maps framework phases to business operations
- custom modules can expose `config.phases` so each entity decides when the
  module's configured handlers should run

Return without doing work when the current phase is not relevant. Throw a `CoredeuxStrategyException` or `CoredeuxValidationException` only when the module is configured incorrectly or cannot safely execute.

For example, a workflow module can be configured only for `after-save`:

```yaml
modules:
  - name: workflows
    enabled: true
    handlers:
      - customerApprovalWorkflow
    config:
      phases:
        - after-save
```

With that configuration, Coredeux may still dispatch the `workflows` module
during other phases, but the handler's `shouldRun(...)` method returns without
executing the workflow beans. This lets the framework keep module dispatch
generic while the module keeps control over its own phase policy.

## 7. Register Through Spring

For core modules, `@Component` is enough when the class is under component scanning.

The strategy layer receives all `List<CoredeuxEntityModuleHandler>` beans and indexes them by module name.

You normally do not need to edit `DefaultCoredeuxStrategy` or `DefaultCoredeuxModuleService` for a new module. They already execute any enabled module that has a matching handler bean.

## 8. Add Tests

Add unit tests under `modules/coredeux-core/src/test/java`.

At minimum, cover:

- `getModuleName()` returns the configured YAML name
- disabled or irrelevant phases do not execute user handlers
- blank handler names are ignored
- configured handler beans are resolved by Spring bean name
- generic entity type mismatches fail with `CoredeuxStrategyException`
- invalid module `config` fails with a useful message
- valid module `config` executes the expected handlers

If the module affects strategy behavior, also add or update strategy tests to prove it runs during the intended CRUD path.

For externally invoked behavior, add coverage through `DefaultCoredeuxModuleService`.

## 9. Document The User-Facing Configuration

Update `docs/features/modules.md` with:

- purpose of the new module
- public contract applications must implement
- YAML example
- relevant phases or operations
- failure behavior

If the module has enough detail for its own guide, add a focused guide under `docs/guides`.

Platform implementation notes should stay under `docs/platform`.

## 10. Run The Core Test Suite

Run the core module tests before merging:

```bash
mvn -pl modules/coredeux-core test
```

If the module touches YAML loading, strategy execution, or public extension contracts, prefer the clean test run:

```bash
mvn -pl modules/coredeux-core clean test
```

## Checklist

- Module name is stable and unique.
- Public extension interface exists when applications need to provide behavior.
- `CoredeuxEntityModuleHandler` implementation is a Spring bean.
- Handler filters irrelevant phases without failing.
- Handler validates module config before executing.
- Handler resolves configured beans by name from `handlers`.
- Handler validates generic entity support when using typed user contracts.
- Tests cover happy path, skipped path, invalid config, and bean resolution failure.
- `docs/features/modules.md` documents user-facing configuration.

<!-- docs-nav-start -->
[Previous: coredeux-core Reference](reference.md) | [Documentation Home](../../README.md) | [Tutorial Order](../../SUMMARY.md) | [Next: coredeux-core-jpa Reference](../core-jpa/reference.md)
<!-- docs-nav-end -->
