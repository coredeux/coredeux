# First 10 Minutes With Coredeux

<!-- docs-nav-start -->
[Previous: Guides](README.md) | [Documentation Home](../README.md) | [Tutorial Order](../SUMMARY.md) | [Next: Docker Demo Setup](docker-demo.md)
<!-- docs-nav-end -->

This guide is the fastest way to feel what Coredeux is trying to do.

You are going to start the demo stack, open the API, run a few prepared
requests, import real sample files, and watch the same framework path handle
CRUD, import, file parsing, validation, persistence, and export.

The goal is not to configure every detail by hand. The goal is to see the shape
of the framework quickly.

## The Story

Imagine you have an enterprise application with a normal product catalog, some
relational data, an audit trail in MongoDB, catalog search in Elasticsearch, and
session-like data in Redis.

Usually, each of those paths grows its own controller style, validation style,
import format, persistence code, and operational rules.

Coredeux gives those paths one shared framework shape:

```text
entity definition -> lifecycle context -> modules -> backend adapter
```

In this tutorial, the demo app lets you touch that shape without building it
from scratch.

## What You Need

Install Docker Desktop, or Docker Engine with Docker Compose.

That is enough for the quick start. The Docker build creates the demo
application image, and Docker Compose starts the backing services:

- PostgreSQL
- MongoDB
- Redis
- Elasticsearch
- `coredeux-demo`

Install Java 17 and Maven only when you want to run tests from your host
machine or work directly on the source.

## 1. Start The Demo

From the repository root:

```powershell
docker compose -f examples/coredeux-demo/docker-compose.yml up --build
```

Wait until the `coredeux-demo` service has started. Then open:

```text
http://localhost:8080/swagger-ui.html
```

You now have a running Coredeux application backed by PostgreSQL, MongoDB,
Redis, and Elasticsearch.

For Docker-specific notes, shutdown commands, and troubleshooting, see
[Docker Demo Setup](docker-demo.md).

## 2. Look At The API

In Swagger UI, notice the main groups:

- `Coredeux Demo CRUD`
- `Coredeux Demo Import`
- `Coredeux Demo File Import`
- `Coredeux Demo Export`

Those are not separate frameworks. They are different ways into the same
Coredeux model.

The demo registers entities from:

```text
examples/coredeux-demo/src/main/resources/coredeux-entities.yml
```

That file tells Coredeux which Java class is managed, which field is the
identifier, which backend adapter is used, and which modules should run.

## 3. Try One CRUD Request

In Swagger UI, open `Coredeux Demo CRUD`.

List products:

```text
GET /api/entities/com.coredeux.demo.domain.Product
```

The demo seeds product data on startup, so you should see records immediately.

Create another product:

```text
POST /api/entities/com.coredeux.demo.domain.Product
```

Use this body:

```json
{
  "sku": "DEMO-PRODUCT-100",
  "name": "Demo Product 100",
  "price": 49.95,
  "documentationUrl": "https://docs.coredeux.dev/demo/products/demo-product-100",
  "category": "SOFTWARE",
  "active": true
}
```

From the outside, this looks like a generic CRUD call. Inside Coredeux, the
request goes through entity definition resolution, lifecycle context creation,
module execution, and the configured backend adapter.

That is the first little spark: the endpoint is generic, but the behavior is
still governed.

## 4. Import The Postman Collections

Swagger is good for discovering the API. Postman is better for running the
prepared demo flows.

Import these two collections into Postman:

```text
examples/coredeux-demo/postman/coredeux-demo.postman_collection.json
examples/coredeux-demo/postman/coredeux-demo-import.postman_collection.json
```

Both collections define:

```text
baseUrl = http://localhost:8080
```

That matches the Docker stack.

Start with the `Coredeux Demo Import API` collection. It has two folders worth
running first:

- `Happy Path`
- `File Imports`

The `Happy Path` folder shows JSON import requests. The `File Imports` folder
shows multipart uploads using the sample files in the repository.

## 5. Run A JSON Import

In Postman, open:

```text
Coredeux Demo Import API / Happy Path / Import Complete Graph
```

Run it.

This single request imports a small graph of related data: roles, products,
customers, addresses, profiles, orders, and order items. It also demonstrates
things that usually become custom one-off code:

- row references
- unique lookup
- custom import value handlers
- collection conversion
- map conversion
- multi-pass import

Then go back to Swagger or Postman and list products again:

```text
GET /api/entities/com.coredeux.demo.domain.Product
```

You should see imported data alongside the startup data.

## 6. Run A File Import

Now use the import-file path.

Sample files live here:

```text
examples/coredeux-demo/samples
```

Included samples:

- `import-products.import`
- `import-products.xlsx`
- `import-jdbc-inventory.import`
- `import-mongodb-audit-trails.import`
- `import-elasticsearch-catalog.import`
- `import-redis-sessions.import`

In Postman, open:

```text
Coredeux Demo Import API / File Imports / Validate Text Import File
```

Run it first. Validation parses the file and checks the import without writing
data.

Then run:

```text
Coredeux Demo Import API / File Imports / Import Text Import File
```

The request uploads:

```text
../samples/import-products.import
```

That relative path works when the collection is imported from
`examples/coredeux-demo/postman`. If Postman cannot resolve it, edit the
request body, choose the `file` form-data field again, and select:

```text
examples/coredeux-demo/samples/import-products.import
```

Run the product list again:

```text
GET /api/entities/com.coredeux.demo.domain.Product
```

You just imported data from a text file through the same Coredeux import model.

## 7. Try The Excel Import

In Postman, run:

```text
Coredeux Demo Import API / File Imports / Validate Excel Import File
Coredeux Demo Import API / File Imports / Import Excel Import File
```

The request uploads:

```text
../samples/import-products.xlsx
```

It also sends:

```text
sheetName = Products
```

This shows the same import model fed by an Excel workbook instead of a text
`.import` file.

## 8. Swap In The Backend Samples

The other sample files show how the same file-import endpoint can feed
different demo paths:

```text
import-jdbc-inventory.import
import-mongodb-audit-trails.import
import-elasticsearch-catalog.import
import-redis-sessions.import
```

To try one, duplicate a Postman file-import request and replace the `file`
form-data value with one of those files.

This is the second spark: the import parser does not need to become a separate
mini-application for every backend. The sample file declares the entity and
columns; Coredeux routes the work through the configured entity definition and
backend adapter.

## 9. Queue An Export

Now close the loop by exporting data.

In Swagger UI or Postman, call:

```text
POST /api/export
```

Example request:

```json
{
  "entity": "com.coredeux.demo.domain.Product",
  "fieldList": [
    { "path": "sku" },
    { "path": "name" },
    { "path": "price" },
    { "path": "category" },
    { "path": "active" }
  ],
  "options": {
    "format": "XLSX",
    "includeHeader": true,
    "fileName": "products.xlsx",
    "storageService": "databaseCoredeuxExportStorageService"
  }
}
```

Check status:

```text
GET /api/export/{uid}
```

Download when complete:

```text
GET /api/export/{uid}/download
```

The export worker runs in the demo application process. If an export stays in
`NEW` or `IN_PROGRESS`, make sure the Docker stack is still running.

## What Just Happened

In a few minutes, you used:

- generic CRUD
- JSON import
- text import-file parsing
- Excel import-file parsing
- multiple backend sample files
- export queueing and download

The important part is not the endpoints themselves. The important part is that
they all lean on the same Coredeux ideas:

- entity definitions describe managed classes
- lifecycle context travels through the framework
- modules add validation, hooks, audit, workflow, or other behavior
- backend adapters handle storage-specific work
- import and export use the same governed application model

That is why Coredeux is useful: the application can grow new capabilities
without every capability inventing its own rules.

## Troubleshooting

If the app is not reachable:

- make sure Docker is running
- make sure the compose stack is still up
- check that port `8080` is free
- check that ports `5432`, `27017`, `6379`, and `9200` are free for the backing
  services

If Postman cannot find a sample file:

- open the request body
- find the `file` form-data field
- manually select the file from `examples/coredeux-demo/samples`

If import validation fails, read the response logs. They identify statement,
row, field, and resolution issues where possible.

## What To Read Next

- [Docker Demo Setup](docker-demo.md)
- [Coredeux Demo Tour](demo-tour.md)
- [Adding A New Entity](add-new-entity.md)
- [Entity Definitions](../configuration/entity-definitions.md)
- [Raw JSON Import Tutorial](../modules/import/guide.md)
- [Import File Tutorial](../modules/import-parser/guide.md)
- [Export Guide](../modules/export/guide.md)

<!-- docs-nav-start -->
[Previous: Guides](README.md) | [Documentation Home](../README.md) | [Tutorial Order](../SUMMARY.md) | [Next: Docker Demo Setup](docker-demo.md)
<!-- docs-nav-end -->
