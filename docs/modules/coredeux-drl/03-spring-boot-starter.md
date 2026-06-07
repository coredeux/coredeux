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

## Spring Registry Access

With the Spring starter, the default `CoredeuxComponentRegistry` implementation
is backed by the Spring `ApplicationContext`.

That gives rule code the same convenience as the earlier Spring-based design:
rules can resolve beans on demand instead of having every dependency injected
explicitly into the DRL service.

## When To Use It

Use the starter when:

- you already have a Spring Boot application
- you want DRL to use Spring-managed beans and services
- you want the same runtime behavior as the native module, but with Spring
  auto-configuration

<!-- docs-nav-start -->
[Previous: Native Runtime](/coredeux-drl-native-runtime) | [Documentation Home](/) | [Next: Reference](/coredeux-drl-reference)
<!-- docs-nav-end -->
