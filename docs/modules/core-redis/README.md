# Coredeux Core Redis

<!-- docs-nav-start -->
[Previous: Coredeux Core MongoDB](../core-mongodb/reference.md) | [Documentation Home](../../README.md) | [Tutorial Order](../../SUMMARY.md) | [Next: Coredeux Import](../import/README.md)
<!-- docs-nav-end -->

`coredeux-core-redis` provides a Redis-backed implementation of
`CoredeuxDataAccessService`.

Use it when you want a lightweight key-value adapter that still follows the
Coredeux lifecycle, validation, and module wiring model. The implementation is
scan-based and JSON-backed, so it is useful for demos, bounded datasets, and
simple operational stores where you want Redis to participate in the Coredeux
contract.

This module is a storage adapter, not a cache abstraction. It does not yet
implement TTL-based eviction, cache invalidation, or Redis-native indexing.

Open the full reference in [reference.md](reference.md).

<!-- docs-nav-start -->
[Previous: Coredeux Core MongoDB](../core-mongodb/reference.md) | [Documentation Home](../../README.md) | [Tutorial Order](../../SUMMARY.md) | [Next: Coredeux Import](../import/README.md)
<!-- docs-nav-end -->
