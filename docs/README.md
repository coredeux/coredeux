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
- [What Is Coredeux?](01-what-is-coredeux.md): the framework thesis and the
  AI-agent-native direction behind it.
- [Getting Started In 10 Minutes](02-getting-started-in-10-minutes.md): the
  Docker-first path into the Spring Boot demo, CRUD, import, file import, and
  export.
- [Tour Of The Demo](03-tour-of-the-demo.md): a guided map of the Spring Boot
  demo and the patterns it is teaching.
- [Vision](overview/vision.md): why Coredeux exists and what it is trying to
  become.
- [Core Reference](modules/coredeux-core/13-reference.md): framework model,
  core flow, lifecycle vocabulary, module system, and extension direction.
- [Adding A New Entity](modules/coredeux-core/11-add-new-entity.md): practical
  workflow for adding an entity to Coredeux.

## Documentation Map

For generated sites, use [SUMMARY.md](SUMMARY.md) as the ordered sidebar or
chapter list. The section map below groups the same material by reader intent.

### Overview

Product and project direction.

- [Vision](overview/vision.md)
- [Roadmap](overview/roadmap.md)

### Core Model

Framework internals and conceptual model.

- [What Is Coredeux?](01-what-is-coredeux.md)
- [Coredeux Core Reference](modules/coredeux-core/13-reference.md)

### Modules

Documentation for optional or standalone Coredeux modules.

- [Coredeux Core](modules/coredeux-core/README.md)
- [Coredeux Core Overview](modules/coredeux-core/01-overview.md)
- [Coredeux Core Lifecycle](modules/coredeux-core/02-lifecycle.md)
- [Coredeux Entity Definitions](modules/coredeux-core/03-entity-definitions.md)
- [Coredeux External Entity Definition Source](modules/coredeux-core/04-external-entity-definition-source.md)
- [Coredeux Module System](modules/coredeux-core/05-modules.md)
- [Add Or Choose A Data Access Service](modules/coredeux-core/06-add-data-access-service.md)
- [Available Data Access Implementations](modules/coredeux-core/07-available-data-access-implementations.md)
- [Add A Hook](modules/coredeux-core/08-add-hook.md)
- [Add A Validator](modules/coredeux-core/09-add-validator.md)
- [Add Audit](modules/coredeux-core/10-add-audit.md)
- [Adding A New Entity](modules/coredeux-core/11-add-new-entity.md)
- [Adding A Core Module](modules/coredeux-core/12-add-core-module.md)
- [Coredeux Core Reference](modules/coredeux-core/13-reference.md)
- [Core JPA Reference](modules/coredeux-core/14-core-jpa-reference.md)
- [Core JDBC Reference](modules/coredeux-core/15-core-jdbc-reference.md)
- [Core Elasticsearch Reference](modules/coredeux-core/16-core-elasticsearch-reference.md)
- [Core MongoDB Reference](modules/coredeux-core/17-core-mongodb-reference.md)
- [Core Redis Reference](modules/coredeux-core/18-core-redis-reference.md)
- [Import Overview](modules/coredeux-import/01-overview.md)
- [Import Reference](modules/coredeux-import/04-reference.md)
- [Export Overview](modules/coredeux-export/01-overview.md)

### Guides

Task-oriented developer guides.

- [Getting Started In 10 Minutes](02-getting-started-in-10-minutes.md)
- [Tour Of The Demo](03-tour-of-the-demo.md)

### Miscellaneous

Companion docs for the plain Java native demo.

- [Getting Started With The Native Demo In 10 Minutes](miscellaneous/01-native-getting-started-in-10-minutes.md)
- [Tour Of The Native Demo](miscellaneous/02-native-tour-of-the-demo.md)

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

- `modules/coredeux-core/`
- `modules/`
- `platform/`

Use `development/session-history/` only when you need historical context about
why a feature evolved in a certain direction.

<!-- docs-nav-start -->
[Documentation Home](README.md) | [Tutorial Order](SUMMARY.md) | [Next: Tutorial Order](SUMMARY.md)
<!-- docs-nav-end -->
