# Docker Demo Setup

<!-- docs-nav-start -->
[Previous: First 10 Minutes With Coredeux](first-10-minutes.md) | [Documentation Home](../README.md) | [Tutorial Order](../SUMMARY.md) | [Next: Coredeux Demo Tour](demo-tour.md)
<!-- docs-nav-end -->

This guide explains how to run `coredeux-demo` with Docker.

Use it when you want a single local stack instead of installing and managing
each dependency by hand.

## Why Docker

The demo now touches multiple backing services across the repository:

- PostgreSQL for the main demo application
- MongoDB for the MongoDB adapter
- Redis for the Redis adapter
- Elasticsearch for the Elasticsearch adapter

Running them manually on every machine quickly becomes more work than the demo
should ask for. Docker keeps the setup reproducible.

## Install Docker

### Windows

Install Docker Desktop and make sure WSL 2 integration is enabled if your
machine uses it.

After installation, verify:

```powershell
docker --version
docker compose version
```

### macOS

Install Docker Desktop for Mac.

After installation, verify:

```bash
docker --version
docker compose version
```

### Linux

Install the Docker Engine and the Docker Compose plugin for your distribution.

After installation, verify:

```bash
docker --version
docker compose version
```

## Run The Demo Stack

From the repository root:

```powershell
docker compose -f examples/coredeux-demo/docker-compose.yml up --build
```

That starts:

- `postgres` for the running demo application
- `mongodb`
- `redis`
- `elasticsearch`
- `coredeux-demo`

When startup completes, open:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Stop The Stack

```powershell
docker compose -f examples/coredeux-demo/docker-compose.yml down
```

## Notes

- The current demo application still uses PostgreSQL as its active runtime
  backing store.
- MongoDB, Redis, and Elasticsearch are included in the Docker stack so the
  backend examples in the repository have a ready local environment.
- If you want the non-PostgreSQL backends to become active demo profiles, that
  can be added later without changing this Docker entry point.

## Related Reading

- [First 10 Minutes With Coredeux](first-10-minutes.md)
- [Coredeux Demo Tour](demo-tour.md)
- [Coredeux Demo README](../../examples/coredeux-demo/README.md)

<!-- docs-nav-start -->
[Previous: First 10 Minutes With Coredeux](first-10-minutes.md) | [Documentation Home](../README.md) | [Tutorial Order](../SUMMARY.md) | [Next: Coredeux Demo Tour](demo-tour.md)
<!-- docs-nav-end -->
