# Coredeux Core JDBC Reference

<!-- docs-nav-start -->
[Previous: Coredeux Core JPA](/modules/coredeux-core/14-core-jpa-reference) | [Documentation Home](/) | [Next: Coredeux Core Elasticsearch](/modules/coredeux-core/16-core-elasticsearch-reference)
<!-- docs-nav-end -->

`coredeux-core-jdbc` is the direct SQL adapter for Coredeux.

It maps entity fields to table columns using standard Java bean conventions and
optional JPA annotations such as `@Table`, `@Column`, and `@Id`.

This page also carries the quick module-level summary that used to live in the
older README, so the overview and the detailed adapter reference stay together.

Use it when you want relational storage without JPA. The adapter works with
plain SQL, JDBC template-style inputs, and data sources.

## What It Supports

- `load`, `save`, `update`, `remove`, and `refresh`
- structured search through `loadAll`
- raw SQL through `query`
- supported comparators that fit typical SQL-backed filtering

## How It Maps

- table name: `@Table(name = "...")`, or the entity simple name if omitted
- column name: `@Column(name = "...")`, or the field name if omitted
- identifier: `@Id`, or a field named `id`

The adapter is best suited to flat relational entities with bean-style getters
and setters. If you need richer relational behavior, use JPA instead.

## Comparator Support

The JDBC adapter supports standard SQL comparators such as:

- `EQUALS`
- `NOTEQUALS`
- `STARTSWITH`
- `ANYWHERE`
- `ANYWHERECS`
- `LESSTHAN`
- `LESSTHANOREQUAL`
- `GREATERTHAN`
- `GREATERTHANOREQUAL`
- `ISNULL`
- `ISNOTNULL`
- `ISEMPTY`
- `ISNOTEMPTY`
- `CONTAINS`
- `NOTCONTAINS`

## Query Style

Use SQL with named parameters.

The adapter wraps the query for counting and applies `LIMIT` / `OFFSET` style
paging when paging is enabled.

The selected columns should map back to the entity properties, either by using
matching column names or by aliasing the columns to the bean property names.

## Default Bean Name

`defaultCoredeuxJdbcDataAccessService`

## Next Step

Use this adapter when you want plain SQL with the Coredeux lifecycle and module
model around it.

<!-- docs-nav-start -->
[Previous: Coredeux Core JPA](/modules/coredeux-core/14-core-jpa-reference) | [Documentation Home](/) | [Next: Coredeux Core Elasticsearch](/modules/coredeux-core/16-core-elasticsearch-reference)
<!-- docs-nav-end -->
