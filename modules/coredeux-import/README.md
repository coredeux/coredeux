# coredeux-import

`coredeux-import` is the future import and impex-style module for Coredeux.

This module depends on `coredeux-core` and is intended to build higher-level import capabilities on top of the framework lifecycle, module system, and data-access abstraction.

## Dependency Model

`coredeux-import` depends directly on:

- `coredeux-core`

Applications using this module are expected to also include the persistence adapter module that matches their storage choice, for example:

- `coredeux-core-jpa`
- future `coredeux-core-mongodb`

That keeps the import module persistence-agnostic while still allowing it to operate through the selected `CoredeuxDataAccessService` implementation available in the application context.

## Status

This module is currently scaffolded and ready for the upcoming import/impex implementation work.
