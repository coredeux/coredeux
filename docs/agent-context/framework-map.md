# Coredeux Framework Map

## Core Contracts

- `modules/coredeux-core`: entity definitions, registry, strategy, lifecycle,
  services, component resolution, validation, and data-access contracts.
- `modules/coredeux-drl`: dynamic runtime logic execution and Coredeux bridges.
- `modules/coredeux-import`: structured import parsing and execution.
- `modules/coredeux-export`: export orchestration and output generation.

## Data Access

- Native implementations live in `modules/coredeux-core-*`.
- Spring Boot integration lives in
  `modules/spring-boot-starters/coredeux-*-spring-boot-starter`.
- `CoredeuxDataAccessService` is the common storage contract.
- Entity definitions select per-entity storage behavior.

## Documentation

- `docs/modules/`: module-specific contracts and usage.
- `docs/overview/`: architecture and concepts.
- `examples/`: executable reference applications.
