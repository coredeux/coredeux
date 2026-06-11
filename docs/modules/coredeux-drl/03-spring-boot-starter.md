# Spring Boot Starter

<!-- docs-nav-start -->
[Previous: Native Runtime](/coredeux-drl-native-runtime) | [Documentation Home](/) | [Next: Reference](/coredeux-drl-reference)
<!-- docs-nav-end -->

The Spring Boot starter is the easiest way to use `coredeux-drl` in a Spring
application.

It auto-configures:

- `ClasspathDRLSourceResolver`
- `DRLService`
- `SpringCoredeuxComponentRegistry` through the Coredeux core starter

That means most Spring applications only need the starter on the classpath and
the DRL file in the expected location.

## Defaults

The starter uses classpath-based rule lookup by default:

```properties
coredeux.drl.classpath-prefix=rules/
coredeux.drl.classpath-suffix=.drl
```

The starter also applies compiler overrides from Spring configuration before
the DRL runtime boots:

```properties
coredeux.drl.java-compiler=NATIVE
coredeux.drl.java-language-level=19
```

That means Spring Boot users can keep the runtime defaults while still
overriding them in `application.properties` or `application.yml`.

If you do not provide a language-level override, Coredeux resolves the DRL
level from the running Java version. The current automatic map is:

- Java 17 -> DRL 17
- Java 18 -> DRL 17
- Java 19 -> DRL 19
- Java 20 through Java 25 -> DRL 19

That keeps the framework usable on Java 17 and above while staying inside the
verified Drools surface area.

## What The Starter Overrides

The DRL starter loads before the core starter so the DRL-aware beans win the
auto-configuration race.

That means Spring applications get:

- `DefaultDRLCoredeuxStrategy`
- `DRLValidatorsModuleHandler`
- `DRLHooksModuleHandler`
- `DRLAuditModuleHandler`
- `DefaultDRLService`

The core starter still provides the shared infrastructure such as the Spring
component registry, request context wiring, and the generic Coredeux service
contracts. The DRL starter simply swaps the strategy and module handlers for
the DRL-backed versions.

Example `application.yml`:

```yaml
coredeux:
  drl:
    classpath-prefix: rules/
    classpath-suffix: .drl
    java-compiler: NATIVE
    java-language-level: "19"
```

The `examples/coredeux-drl-spring-boot-demo` module shows this in a real
application with:

- DRL-backed CRUD
- native and DRL validators side by side
- native and DRL hooks side by side
- native and DRL audit handlers side by side
- database-backed DRL source storage and conversion endpoints

The same authoring rule applies in Spring: each DRL source should be
self-contained after conversion. Helper methods in the Java source are useful
for IDE authoring, but they are not reusable DRL methods once the source is
converted.

## Property Precedence

The starter reads Spring configuration at startup and applies it to the DRL
runtime before rule execution begins.

Practical precedence is:

1. values provided in Spring `application.properties` or `application.yml`
2. any pre-existing system property
3. the runtime-selected default for the current Java version (`17` or `19`)

That lets you override behavior without changing code, while still keeping the
same defaults in native hosts and tests.

## Spring Registry Access

With the Spring starter, the default `CoredeuxComponentRegistry` implementation
is backed by the Spring `ApplicationContext`.

That gives rule code the same convenience as the earlier Spring-based design:
rules can resolve beans on demand instead of having every dependency injected
explicitly into the DRL service.

The important part is that the rules ask for capabilities, not for the Spring
container itself. That keeps the rule source clean and makes the same rule text
work in both native and Spring environments.

Example rule usage:

```drl
global com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;

rule "check"
when
   $context : com.coredeux.drl.model.RuleContext(method == "check")
then
   com.example.SampleService sampleService =
       componentRegistry.getComponent("sampleService", com.example.SampleService.class);
   $context.setOutput(sampleService.message());
end
```

When you write the Java-side execution context in Spring code, use the typed
form too:

```java
RuleContext<String> context = RuleContext.method("check")
        .fact(sampleEntity);
```

## When You Should Override The Starter

You can override the starter's defaults by providing your own beans if you need
to:

- resolve DRL from a different source
- replace the DRL service implementation
- use a different component registry strategy

If you only need custom rule text location or compiler settings, properties are
usually the simplest option.

## When To Use It

Use the starter when:

- you already have a Spring Boot application
- you want DRL to use Spring-managed beans and services
- you want the same runtime behavior as the native module, but with Spring
  auto-configuration

## What A Spring Developer Should Remember

- the starter gives you runtime convenience, not a different rule language
- the rule execution flow is still the same `ruleId -> source -> compile -> cache -> execute`
- Spring only changes how the registry and configuration are supplied
- if the external rule source changes, purge the runtime cache just like you
  would in native mode

## Hands-On Pages

For full examples of each module style, continue to:

- [Data Access With DRL](/coredeux-drl-data-access)
- [Validators With DRL](/coredeux-drl-validators)
- [Hooks With DRL](/coredeux-drl-hooks)
- [Audit With DRL](/coredeux-drl-audit)
- [Custom Handlers](/coredeux-drl-custom-handlers)

<!-- docs-nav-start -->
[Previous: Native Runtime](/coredeux-drl-native-runtime) | [Documentation Home](/) | [Next: Reference](/coredeux-drl-reference)
<!-- docs-nav-end -->
