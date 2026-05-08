# Coredeux Core Elasticsearch

<!-- docs-nav-start -->
[Previous: Coredeux Core JDBC](../core-jdbc/reference.md) | [Documentation Home](../../README.md) | [Tutorial Order](../../SUMMARY.md) | [Next: Coredeux Core MongoDB](../core-mongodb/reference.md)
<!-- docs-nav-end -->

`coredeux-core-elasticsearch` provides an Elasticsearch-backed implementation of
`CoredeuxDataAccessService`.

Use it when you want a search-oriented backend that still follows Coredeux's
entity lifecycle, validation, and module wiring. The adapter is backed by
`ElasticsearchOperations` and exposes Elasticsearch-aware search behavior
without requiring the rest of the application to speak Elasticsearch directly.

Open the full reference in [reference.md](reference.md).

<!-- docs-nav-start -->
[Previous: Coredeux Core JDBC](../core-jdbc/reference.md) | [Documentation Home](../../README.md) | [Tutorial Order](../../SUMMARY.md) | [Next: Coredeux Core MongoDB](../core-mongodb/reference.md)
<!-- docs-nav-end -->
