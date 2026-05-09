# Coredeux Documentation

<!-- docs-nav-start -->
[Documentation Home](README.md) | [Tutorial Order](SUMMARY.md) | [Next: Tutorial Order](SUMMARY.md)
<!-- docs-nav-end -->

This is the documentation entry point for Coredeux. The docs are organized by
reader intent: learn the framework, understand internals, configure an
application, use a module, extend the platform, or maintain the project.

## Start Here

- [Tutorial Order](SUMMARY.md): the canonical previous/next sequence for
  generated HTML documentation.
- [First 10 Minutes With Coredeux](guides/first-10-minutes.md): the shortest
  Docker-first path from a running demo stack to CRUD, modules, import, and
  export.
- [Docker Demo Setup](guides/docker-demo.md): the containerized stack details
  for the demo application and its backing services.
- [Vision](overview/vision.md): why Coredeux exists and what it is trying to
  become.
- [Architecture Overview](architecture/overview.md): framework model, core
  flow, lifecycle vocabulary, and extension direction.
- [Adding A New Entity](guides/add-new-entity.md): practical application
  workflow for adding an entity to Coredeux.

## Documentation Map

For generated sites, use [SUMMARY.md](SUMMARY.md) as the ordered sidebar or
chapter list. The section map below groups the same material by reader intent.

### Overview

Product and project direction.

- [Vision](overview/vision.md)
- [Roadmap](overview/roadmap.md)

### Architecture

Framework internals and conceptual model.

- [Architecture Overview](architecture/overview.md)
- [Lifecycle Model](architecture/lifecycle.md)

### Configuration

Application-facing configuration contracts.

- [Entity Definitions](configuration/entity-definitions.md)
- [External Entity Definition Sources](configuration/external-entity-definition-source.md)

### Features

Cross-cutting framework feature documentation.

- [Module System](features/modules.md)

### Modules

Documentation for optional or standalone Coredeux modules.

- [Core Module](modules/core/README.md)
- [Core JPA Module](modules/core-jpa/reference.md)
- [Core JDBC Module](modules/core-jdbc/reference.md)
- [Core Elasticsearch Module](modules/core-elasticsearch/README.md)
- [Core MongoDB Module](modules/core-mongodb/reference.md)
- [Core Redis Module](modules/core-redis/reference.md)
- [Import Module](modules/import/guide.md)
- [Import Parser Module](modules/import-parser/guide.md)
- [Export Module](modules/export/guide.md)

### Guides

Task-oriented developer guides.

- [First 10 Minutes With Coredeux](guides/first-10-minutes.md)
- [Docker Demo Setup](guides/docker-demo.md)
- [Coredeux Demo Tour](guides/demo-tour.md)
- [Adding A New Entity](guides/add-new-entity.md)

### Platform

Contributor guides for changing Coredeux itself.

- [Platform Documentation](platform/README.md)

### Project

Governance, release, and operational project docs.

- [Governance](project/governance.md)
- [Release Process](project/release-process.md)
- [Security Policy](project/security.md)

### Development Notes

Historical implementation notes and handoff material. These are useful for
context, but they are not canonical user documentation.

- [Import Module Session History](development/session-history/import-module.md)

## Canonical Docs Versus Notes

Use canonical docs for implementation and usage decisions:

- `architecture/`
- `configuration/`
- `features/`
- `modules/`
- `guides/`
- `platform/`

Use `development/session-history/` only when you need historical context about
why a feature evolved in a certain direction.

<!-- docs-nav-start -->
[Documentation Home](README.md) | [Tutorial Order](SUMMARY.md) | [Next: Tutorial Order](SUMMARY.md)
<!-- docs-nav-end -->
