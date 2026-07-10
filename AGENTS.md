# Coredeux Agent Guide

Coredeux is the foundational framework. Keep its guidance generic and
independent of downstream frameworks, products, and application
implementations.

## Read First

- `docs/agent-context/knowledge-policy.md`
- `docs/agent-context/framework-map.md`
- `docs/agent-context/invariants.md`
- `docs/agent-context/manifest.yaml`
- module documentation under `docs/modules/`

## Working Rules

- Preserve public contracts and backend neutrality.
- Prefer module-local patterns and existing extension interfaces.
- Keep native modules separate from Spring Boot starter wiring.
- Structured data must use structured parsers and APIs.
- Add tests at the contract level and for affected backend implementations.
- Documentation changes are part of framework-significant work.

## Knowledge Boundary

Downstream frameworks, products, and applications may reveal useful Coredeux
improvements, but they do not define Coredeux behavior.

Promote a lesson into Coredeux guidance only when it is generalized, owned by
Coredeux, and supported by an adopted contract or implementation.