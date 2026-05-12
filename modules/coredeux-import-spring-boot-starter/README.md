# Coredeux Import Spring Boot Starter

This module is the Spring Boot bridge for `coredeux-import`.

`coredeux-import` stays plain Java. The starter contributes Spring Boot
auto-configuration that creates the import infrastructure beans and lets a
Spring application provide its own domain, data-access, and custom handler
beans.

The starter wires:

- `CoredeuxTextImportParser`
- `CoredeuxExcelImportParser`
- default import value handlers
- `ImportValueHandlerResolver`
- `ImportEntityTargetService`
- `CoredeuxImportService`

The starter expects the core starter to be present as well, so the same Spring
application can use the Coredeux runtime and the import pipeline together.
