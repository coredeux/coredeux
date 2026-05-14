# Coredeux Vision

<!-- docs-nav-start -->
[Previous: Overview](README.md) | [Documentation Home](../README.md) | [Next: Roadmap](roadmap.md)
<!-- docs-nav-end -->

Coredeux is a framework for enterprise Java applications that need a stable
way to handle the common work around business intent.

Today that means helping teams build applications with:

- CRUD and search
- validation and hooks
- audit and workflow-style extensions
- import and export
- consistent storage routing across SQL and NoSQL systems
- clean integration into both Spring Boot apps and plain Java hosts

The longer-term direction is to make those same enterprise building blocks
natural for AI agents to discover and use through governed contracts such as
MCP.

That is the dual role of Coredeux:

1. provide reusable building blocks for enterprise applications
2. embed cleanly into existing Java and Spring systems without forcing a rewrite

The reason this matters in the age of Codex and Claude is simple: agents can
write code, but enterprise systems still need stable framework behavior after
the code is shipped. Coredeux is meant to keep that behavior consistent.

## Why It Exists

Most applications end up rebuilding the same framework-layer patterns:

- create, update, fetch, and remove flows
- validation
- lifecycle hooks
- audit
- import and export
- backend-specific storage routing

Coredeux standardizes those pieces so the application can focus on business
intent instead of wiring repeatable infrastructure in every service.

## Who It Serves

Coredeux is relevant for:

- teams building new enterprise Java applications
- teams extending existing Spring Boot systems
- teams that want a clear path toward agent-facing runtime integration
- teams that want the same model to work across SQL and NoSQL storage

## Long-Term Direction

The framework is designed so future agent-aware features can live inside the
same operational model as the rest of the app. That means Coredeux is not just
about code generation. It is about the runtime shape that keeps enterprise
behavior predictable when agents are involved.

<!-- docs-nav-start -->
[Previous: Overview](README.md) | [Documentation Home](../README.md) | [Next: Roadmap](roadmap.md)
<!-- docs-nav-end -->
