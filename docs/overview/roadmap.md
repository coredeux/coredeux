# Coredeux Roadmap

<!-- docs-nav-start -->
[Previous: Vision](/overview/vision) | [Documentation Home](/) | [Next: Modules](/modules/)
<!-- docs-nav-end -->

Coredeux is being built in stages so the framework stays usable while the
surface area grows.

## Phase 1 - Foundation

- core lifecycle and strategy model
- entity-definition loading from YAML
- module dispatch for validators, hooks, audit, and custom modules
- SQL and NoSQL data-access adapters
- Spring Boot starters for the Spring-based host path
- a plain Java native host path for the non-Spring route

## Phase 2 - Framework Completeness

- richer module coverage for enterprise app behavior
- stronger import/export and workflow-style patterns
- clearer support for custom data access and backend-specific behavior
- tighter demo parity between Spring Boot and native hosts

## Phase 3 - Agent-Aware Runtime

- MCP-facing contracts for discoverable framework operations
- agent-readable tool surfaces for common enterprise actions
- clearer governance, traceability, and operational boundaries for agent use
- future modules where agents participate from inside the application runtime

## Long-Term Goal

Coredeux aims to be the framework layer that keeps enterprise applications
predictable even as implementation shifts between humans, agents, Spring Boot,
and native Java.

The target is not to replace application code. The target is to give that code
a stable structure that survives long-lived systems and repeated agent-assisted
changes.

<!-- docs-nav-start -->
[Previous: Vision](/overview/vision) | [Documentation Home](/) | [Next: Modules](/modules/)
<!-- docs-nav-end -->
