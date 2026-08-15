# Coredeux Invariants

- `CoredeuxService` is the application-facing CRUD/search contract.
- Strategies coordinate storage, validation, hooks, audit, and modules.
- `CoredeuxDataAccessService` implementations must preserve contract semantics
  across backends.
- Entity definitions own identifiers, storage selection, and module handlers.
- Global configuration is fallback behavior; entity definitions may override it.
- Hooks and validators are extension contracts, not application controllers.
- DRL execution uses `RuleContext` and reports output or exceptions through it.
- Native modules must not acquire accidental Spring dependencies.
- Spring Boot starters wire framework contracts without redefining them.
- Spring Boot data-access starters are opt-in; starter dependencies alone must
  not create datasource-specific adapter beans.
- Import/export formats must be parsed structurally and validated explicitly.
- Contract changes require focused tests and updates to affected implementations
  and documentation.
