# Coredeux DRL

<!-- docs-nav-start -->
[Previous: Coredeux Export](/coredeux-export-reference) | [Documentation Home](/) | [Next: Coredeux DRL Overview](/coredeux-drl-overview)
<!-- docs-nav-end -->

This section documents the Coredeux dynamic runtime logic layer.

It has two practical faces:

1. the plain Java DRL runtime used by native hosts
2. the Spring Boot starter that wires the same runtime into a Spring app

Read it in order if you want the full picture:

1. [Overview](/coredeux-drl-overview)
2. [Native Runtime](/coredeux-drl-native-runtime)
3. [Spring Boot Starter](/coredeux-drl-spring-boot-starter)
4. [Reference](/coredeux-drl-reference)
5. [Data Access With DRL](/coredeux-drl-data-access)
6. [Validators With DRL](/coredeux-drl-validators)
7. [Hooks With DRL](/coredeux-drl-hooks)
8. [Audit With DRL](/coredeux-drl-audit)
9. [Custom Handlers](/coredeux-drl-custom-handlers)

The numbered pages are the canonical version of this section.

For a full working example that combines native and DRL-backed handlers, see
the demo module:

- `examples/coredeux-drl-spring-boot-demo`

That demo is intentionally small and mirrors the real learning from the DRL
POC:

- DRL authoring starts in Java-like source files
- each DRL source file is self-contained after conversion
- helper methods inside the Java authoring class do not survive as reusable
  DRL methods unless you model them as separate rule sources or external
  services
- DRL source packages are split by concern, such as `dataaccess`, `hooks`,
  `validation`, and `audit`

If you want authoring-time Java-to-DRL conversion, use the tools section:

- [Coredeux DRL DevTools](/coredeux-drl-devtools)

<!-- docs-nav-start -->
[Previous: Coredeux Export](/coredeux-export-reference) | [Documentation Home](/) | [Next: Coredeux DRL Overview](/coredeux-drl-overview)
<!-- docs-nav-end -->
