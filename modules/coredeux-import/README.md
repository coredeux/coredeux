# coredeux-import

`coredeux-import` is the import and impex-style module for Coredeux.

This module depends on `coredeux-core` and builds higher-level import capabilities on top of the framework lifecycle, module system, and data-access abstraction. It also contains the text and Excel import parsers that compile author-friendly files into `ImportRequest` objects.

## Dependency Model

`coredeux-import` depends directly on:

- `coredeux-core`
- Jackson databind for JSON/map value handling
- Apache POI for XLS/XLSX import parsing

Applications using this module are expected to also include the persistence adapter module that matches their storage choice, for example:

- `coredeux-core-jpa`
- future `coredeux-core-mongodb`

That keeps the import module persistence-agnostic while still allowing it to operate through the selected `CoredeuxDataAccessService` implementation available to the application.

The module is plain Java. It does not require Spring. Spring-aware applications can wire the same services through a starter or their own configuration.

## Status

This module includes:

- raw JSON import request and response contracts
- validation-only and execution paths
- create, upsert, modify, delete, and fetch operations
- unique, lookup, and query-based existing-record resolution
- references, row keys, collection modes, macros, and custom value handlers
- pipe-separated text import parsing
- XLS/XLSX import parsing
