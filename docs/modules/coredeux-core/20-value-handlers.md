# Coredeux Value Handlers

<!-- docs-nav-start -->
[Previous: Property Resolution Order](/modules/coredeux-core/19-property-resolution-order) | [Documentation Home](/) | [Next: Coredeux Import](/modules/coredeux-import/01-overview)
<!-- docs-nav-end -->

This page explains the shared value-handler contract used by Coredeux import,
export, and DRL-aware flows.

The important idea is simple: a value handler converts one field payload into
another value. Coredeux keeps the handler contract small so the same pattern can
be used in native applications, Spring Boot applications, and DRL-backed
handlers.

## The Contracts

The core module defines three pieces:

- `ValueContext`
- `CoredeuxValueHandler<T, V extends ValueContext>`
- `CoredeuxValueHandlerService`

The default runtime implementation is `DefaultCoredeuxValueHandlerService`.

### `ValueContext`

`ValueContext` is intentionally empty.

It is a marker type that says, "this object is the payload passed to a value
handler." The context itself should stay data-only:

- do not put service locators inside it
- do not add framework behavior to it
- do not treat it like a dependency container

Feature modules extend this marker with their own payload classes, for example:

- `ImportValueContext`
- `ExportValueContext`

### `CoredeuxValueHandler<T, V>`

This is the actual handler contract:

```java
public interface CoredeuxValueHandler<T, V extends ValueContext> {
    T handle(V context);
}
```

The generic return type lets the same pattern support different kinds of
conversions:

- strings
- numbers
- dates
- domain references
- collections
- custom application-specific values

### `CoredeuxValueHandlerService`

This is the shared invocation contract:

```java
public interface CoredeuxValueHandlerService {

    <T, V extends ValueContext> T invoke(String handler, V context);
}
```

The service resolves the handler by name and passes the context through.

In the default native implementation, handler resolution goes through the
`CoredeuxComponentRegistry`. That means the handler name is normally the bean
name.

## How The Default Service Works

`DefaultCoredeuxValueHandlerService` does three things:

1. validates the handler name
2. resolves the handler from the component registry
3. calls `handle(context)` on the resolved handler

That gives native applications a very small and predictable extension model.

If the registry cannot resolve the handler, the service throws
`CoredeuxValueHandlerException`.

## Implementing A Native Handler

The simplest implementation is a Spring bean that implements the relevant
handler contract.

Example for import:

```java
@Component("legacyDateImportHandler")
public class LegacyDateImportHandler implements CoredeuxImportValueHandler {

    @Override
    public Object handle(ImportValueContext context) {
        if (context.getEffectiveValue() == null || context.getEffectiveValue().isBlank()) {
            return null;
        }
        return parseDate(context.getEffectiveValue(), context.getColumn().getMetadata());
    }
}
```

Example for export:

```java
@Component("dateFormatExportHandler")
public class DateFormatExportHandler implements CoredeuxExportValueHandler {

    @Override
    public Object handle(ExportValueContext context) {
        Object value = context.getResolvedValue();
        if (value == null) {
            return "";
        }
        return formatDate((Date) value, context.getField().getMetadata());
    }
}
```

In both cases:

- the handler stays focused on conversion
- the context carries the current field state
- dependencies are injected into the handler bean itself, not into the context

## Using The Service Directly

Applications can invoke handlers without knowing the underlying implementation.

```java
ImportValueContext importContext = ImportValueContext.builder()
        .effectiveValue("https://example.com")
        .build();

Object value = valueHandlerService.invoke("demoUriImportHandler", importContext);
```

The same call pattern works for export:

```java
ExportValueContext exportContext = ExportValueContext.builder()
        .resolvedValue(new Date())
        .build();

Object value = valueHandlerService.invoke("dateFormatExportHandler", exportContext);
```

## DRL-Aware Handlers

The DRL starter can replace the default value-handler service with a DRL-aware
implementation. In that mode:

- handler names ending in `.drl` are routed through the DRL runtime
- normal handler names still use the native registry path
- the context object is passed through unchanged as the handler payload

That makes the extension model uniform across native, Spring, and DRL-backed
handlers.

If you need the DRL authoring pattern for import or export handlers, read the
dedicated DRL pages in this section.

## Good Rules Of Thumb

- keep value handlers small and deterministic
- use metadata for handler-specific options, not core framework behavior
- keep `ValueContext` data-only
- use a native handler when the logic is simple and host-specific
- use a DRL-backed handler when you want authoring-time conversion or runtime
  rule replacement

The value-handler model is meant to be the small, shared seam that import,
export, and DRL features can all rely on.

<!-- docs-nav-start -->
[Previous: Property Resolution Order](/modules/coredeux-core/19-property-resolution-order) | [Documentation Home](/) | [Next: Coredeux Import](/modules/coredeux-import/01-overview)
<!-- docs-nav-end -->
