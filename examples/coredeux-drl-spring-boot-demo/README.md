# Coredeux DRL Spring Boot Demo

This is a small DRL proof-of-concept demo.

It keeps the surface area intentionally small:

- one domain model: `Customer`
- one native validator, hook, and audit handler
- one DRL validator, hook, and audit handler
- one DRL-backed JPA data-access source
- one database-backed DRL rule store
- one database-backed entity-definition registry with Redis cache
- REST endpoints for CRUD, DRL conversion, and DRL execution

The DRL source classes under `src/main/java/com/coredeux/demo/drl` are
authoring inputs for the converter. The generated DRL is stored in PostgreSQL
and used by the runtime from there.

The developer flow is:

1. write a normal Java rule source class
2. annotate it with the DRL converter annotations
3. send the source to the conversion endpoint
4. store the generated DRL in PostgreSQL
5. execute the stored DRL at runtime
6. seed the entity-definition registry into PostgreSQL during boot
7. refresh or purge the registry cache through the cache endpoint

## Run

```bash
mvn -pl examples/coredeux-drl-spring-boot-demo -am test
```

```bash
docker compose -f examples/coredeux-drl-spring-boot-demo/docker-compose.yml up --build
```

The Docker stack builds the framework modules and the demo app inside the
container, then runs the packaged Spring Boot jar. It exposes the app on `8080`
and the debugger on `5005`. By default the compose file starts the JVM with a
JDWP agent so you can attach an IDE debugger straight away. The stack also
includes PostgreSQL and Redis, so the registry bootstrapping and cache
endpoints work without any extra setup. If you want a different debug mode,
override `JAVA_TOOL_OPTIONS` when you start compose.

PowerShell:

```powershell
$env:JAVA_TOOL_OPTIONS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005'
docker compose -f examples/coredeux-drl-spring-boot-demo/docker-compose.yml up --build
Remove-Item Env:JAVA_TOOL_OPTIONS
```

To debug the app locally instead of inside Docker, run:

```powershell
mvn -pl examples/coredeux-drl-spring-boot-demo -am spring-boot:run "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
```

## Main endpoints

- `POST /api/entities/customer`
- `PUT /api/entities/customer/{id}`
- `DELETE /api/entities/customer/{id}`
- `GET /api/drl/rules`
- `POST /api/drl/rules/convert`
- `PUT /api/drl/rules/{ruleId}`
- `POST /api/drl/rules/{ruleId}/execute`
- `POST /api/drl/rules/{ruleId}/execute-source`
- `POST /api/drl/execute-source`
- `GET /api/drl/entity-definitions/cache`
- `PUT /api/drl/entity-definitions/cache`
- `POST /api/drl/entity-definitions/cache/refresh`
- `POST /api/drl/entity-definitions/cache/bootstrap`
- `GET /api/drl/cache/{ruleId}`
- `DELETE /api/drl/cache/{ruleId}`
- `DELETE /api/drl/cache`

The DRL runtime source texts are under `src/main/java/com/coredeux/demo/drl`,
with the generated runtime rules stored in the database. The entity-definition
registry is seeded from `src/main/resources/coredeux-entities.yml`, stored in
the `entity_definition_registry` table, and mirrored in Redis for fast reuse.
Use `PUT /api/drl/entity-definitions/cache?sourceLocation=postman` to update
the stored registry directly from Postman with a raw YAML text body and
`Content-Type: text/plain`.
