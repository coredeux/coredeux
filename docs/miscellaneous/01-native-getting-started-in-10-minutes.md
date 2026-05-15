# Getting Started With The Native Demo In 10 Minutes

<!-- docs-nav-start -->
[Previous: Coredeux Export Reference](../modules/coredeux-export/04-reference.md) | [Documentation Home](../README.md) | [Next: Native Tour Of The Demo](02-native-tour-of-the-demo.md)
<!-- docs-nav-end -->

This is the fastest way to meet the Coredeux native demo in a running
application.

The goal is not to explain every class up front. The goal is to start the
plain Java demo, see the HTTP surface, and feel how the framework behaves
without Spring in the middle.

If you are an AI agent reading this, notice how the app is wired through
configuration, runtime composition, and a small embedded server. If you are a
developer, notice how little infrastructure code is needed to expose a real
application surface.

If you want the Spring Boot version of the same starting point, see:

- [Getting Started In 10 Minutes](../02-getting-started-in-10-minutes.md)
- [Tour Of The Demo](../03-tour-of-the-demo.md)

## The Story

Imagine a Java application that still needs PostgreSQL-backed CRUD, import,
export, validation, and lifecycle behavior, but does not want to depend on
Spring Boot to prove the point.

Coredeux gives that app one shared path:

```text
entity definition -> runtime config -> modules -> backend adapter
```

This guide shows the native version of that path in action.

## What You Need

For the quick start, you only need Docker.

If Docker is not already installed, set it up first:

- Windows: install Docker Desktop and make sure WSL 2 integration is enabled if
  your machine uses it
- macOS: install Docker Desktop for Mac
- Linux: install the Docker Engine and the Docker Compose plugin for your
  distribution

After installation, verify the tools:

```powershell
docker --version
docker compose version
```

That is enough because the native demo stack brings up:

- PostgreSQL
- `coredeux-java-native-demo`

If you want to work on the source later, Java 17 and Maven are useful, but they
are not required for the first ten minutes.

## 1. Start The Native Demo Stack

From the repository root:

```powershell
docker compose -f examples/coredeux-java-native-demo/docker-compose.yml up --build
```

Wait for the services to become healthy. When the app is ready, open:

```text
http://localhost:8080/health
```

### Normal Run

This is the default path. No debug flags are needed.

```bash
docker compose -f examples/coredeux-java-native-demo/docker-compose.yml up --build
```

### Debug Run

Use the same compose command, but add a JDWP agent on port `5005`. Start the
demo in this mode, keep that terminal running, and then attach your debugger to
`localhost:5005` from your IDE.

PowerShell:

```powershell
$env:JAVA_TOOL_OPTIONS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005'
docker compose -f examples/coredeux-java-native-demo/docker-compose.yml up --build
Remove-Item Env:JAVA_TOOL_OPTIONS
```

bash or zsh:

```bash
JAVA_TOOL_OPTIONS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005' docker compose -f examples/coredeux-java-native-demo/docker-compose.yml up --build
```

If you prefer local debugging instead of Docker debugging, run the jar from
your IDE or with `java -agentlib:jdwp=...`, then attach to `localhost:5005`.

That health endpoint is the first sign that the native runtime, the Coredeux
configuration, and the embedded HTTP server are all alive together.

## 2. Try The Customer API

The native server exposes a direct customer path:

```text
/api/customers
```

It also exposes the generic Coredeux entity route:

```text
/api/entities/{entityName}
```

Start with the customer path because it is the simplest way to see the flow.

List customers:

```text
GET /api/customers
```

Create a customer:

```text
POST /api/customers
```

Use a small body like:

```json
{
  "name": "Native Demo Customer",
  "email": "native.demo@example.com",
  "active": true,
  "status": "ACTIVE"
}
```

Then load it again, update it, and remove it using the id returned by the
create call.

That is the native equivalent of the Spring Boot demo story: the app surface is
small, but the framework path behind it is still governed.

## 3. Import A Sample File

The native demo includes a sample import file here:

```text
examples/coredeux-java-native-demo/src/main/resources/samples/postgres-customers.import
```

The server can import it through:

```text
POST /api/import/sample
```

It also accepts import requests through:

```text
POST /api/import/validate
POST /api/import
POST /api/import/file
```

That gives you the same basic import story as the Spring Boot demo, but with a
plain Java host instead of Spring MVC.

## 4. Notice The Defaults

The native demo keeps its runtime configuration in:

```text
examples/coredeux-java-native-demo/src/main/resources/META-INF/coredeux.yml
```

That file provides the Coredeux property map. In this demo, it drives things
like:

- the entity-definition location
- the import default parser
- the export default format
- export storage and worker settings

The JPA layer also has its own persistence contract in:

```text
examples/coredeux-java-native-demo/src/main/resources/META-INF/persistence.xml
```

That separation is intentional. The native app stays small because the runtime
contract is explicit.

## 5. Try Export

The native server also exposes export endpoints:

```text
POST /api/export
GET /api/export/{uid}
GET /api/export/{uid}/download
```

This completes the loop:

1. create or load data
2. import or export it
3. keep the runtime behavior inside one compact application host

## What You Should Have Seen

By the time you finish this guide, you should have a feel for the native
framework story:

- one embedded Java host
- one shared lifecycle model
- a direct HTTP surface
- configuration-driven runtime wiring
- PostgreSQL-backed persistence
- import and export as real application flows

That is the native shape of Coredeux.

## What To Read Next

If you want to understand how the native demo is built, continue with:

- [Native Tour Of The Demo](02-native-tour-of-the-demo.md)

<!-- docs-nav-start -->
[Previous: Coredeux Export Reference](../modules/coredeux-export/04-reference.md) | [Documentation Home](../README.md) | [Next: Native Tour Of The Demo](02-native-tour-of-the-demo.md)
<!-- docs-nav-end -->
