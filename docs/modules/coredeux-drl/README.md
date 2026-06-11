# Coredeux DRL

<!-- docs-nav-start -->
[Previous: Coredeux Export](/modules/coredeux-export/04-reference) | [Documentation Home](/) | [Next: Coredeux DRL Overview](/modules/coredeux-drl/01-overview)
<!-- docs-nav-end -->

This section documents the Coredeux dynamic runtime logic layer.

It has two practical faces:

1. the plain Java DRL runtime used by native hosts
2. the Spring Boot starter that wires the same runtime into a Spring app

Read it in order if you want the full picture:

1. [Overview](/modules/coredeux-drl/01-overview)
2. [Native Runtime](/modules/coredeux-drl/02-native-runtime)
3. [Spring Boot Starter](/modules/coredeux-drl/03-spring-boot-starter)
4. [Reference](/modules/coredeux-drl/04-reference)
5. [Data Access With DRL](/modules/coredeux-drl/05-data-access)
6. [Validators With DRL](/modules/coredeux-drl/06-validators)
7. [Hooks With DRL](/modules/coredeux-drl/07-hooks)
8. [Audit With DRL](/modules/coredeux-drl/08-audit)
9. [Custom Handlers](/modules/coredeux-drl/09-custom-handlers)
10. [Import Value Handlers](/modules/coredeux-drl/10-import-value-handlers)
11. [Export Value Handlers](/modules/coredeux-drl/11-export-value-handlers)

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
- the same DRL-backed handler pattern also applies to import and export field
  handlers

If you want authoring-time Java-to-DRL conversion, use the tools section:

- [Coredeux DRL DevTools](/modules/coredeux-drl-devtools/)

<!-- docs-nav-start -->
[Previous: Coredeux Export](/modules/coredeux-export/04-reference) | [Documentation Home](/) | [Next: Coredeux DRL Overview](/modules/coredeux-drl/01-overview)
<!-- docs-nav-end -->
