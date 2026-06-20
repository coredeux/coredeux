package com.coredeux.examples.nativejava;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.drl.converter.DrlConversionException;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.service.DRLService;
import com.coredeux.export.model.ExportFormat;
import com.coredeux.export.model.ExportRequest;
import com.coredeux.export.model.ExportResponse;
import com.coredeux.export.model.ExportStatus;
import com.coredeux.export.service.CoredeuxExportService;
import com.coredeux.export.support.ExportJsonSupport;
import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.model.ImportResponse;
import com.coredeux.impex.parser.excel.CoredeuxExcelImportParser;
import com.coredeux.impex.parser.text.CoredeuxTextImportParser;
import com.coredeux.examples.nativejava.drl.DrlConversionRequest;
import com.coredeux.examples.nativejava.drl.DrlRuleNotFoundException;
import com.coredeux.examples.nativejava.drl.DrlRuleRecord;
import com.coredeux.examples.nativejava.drl.DrlRuleUpsertRequest;
import com.coredeux.examples.nativejava.drl.DrlSourceExecutionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Tiny embedded HTTP server for the native Coredeux demo.
 *
 * <p>
 * The server exposes the same story as the Spring Boot demo: generic entity
 * CRUD, import validation/import, and export queue/status/download.
 * </p>
 */
public final class CoredeuxNativeDemoServer implements AutoCloseable {

    private static final String ENTITY_PATH = "/api/entities";
    private static final String IMPORT_PATH = "/api/import";
    private static final String EXPORT_PATH = "/api/export";
    private static final String DRL_PATH = "/api/drl";
    private static final String DRL_SAMPLE_PATH = "/api/drl/sample";
    private static final String OPENAPI_PATH = "/v3/api-docs";
    private static final String SWAGGER_UI_PATH = "/swagger-ui.html";
    private static final String SAMPLE_IMPORT_PATH = "samples/postgres-customers.import";

    private final CoredeuxNativeRuntime runtime;
    private final HttpServer server;
    private final ExecutorService executorService;
    private final NativeEntityResolver entityResolver;
    private final ObjectMapper objectMapper;

    private CoredeuxNativeDemoServer(CoredeuxNativeRuntime runtime, HttpServer server,
            ExecutorService executorService, NativeEntityResolver entityResolver, ObjectMapper objectMapper) {
        this.runtime = runtime;
        this.server = server;
        this.executorService = executorService;
        this.entityResolver = entityResolver;
        this.objectMapper = objectMapper;
    }

    public static CoredeuxNativeDemoServer start(CoredeuxNativeRuntime runtime, int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        server.setExecutor(executorService);

        CoredeuxNativeDemoServer demoServer = new CoredeuxNativeDemoServer(runtime, server, executorService,
                new NativeEntityResolver(runtime.reflectionHelperService(), runtime.entityDefinitionResolver()),
                ExportJsonSupport.objectMapper());
        demoServer.registerRoutes();
        server.start();
        return demoServer;
    }

    public int port() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
        executorService.shutdownNow();
    }

    private void registerRoutes() {
        server.createContext("/health", this::handleHealth);
        server.createContext(OPENAPI_PATH, this::handleOpenApiDocs);
        server.createContext(SWAGGER_UI_PATH, this::handleSwaggerUi);
        server.createContext(ENTITY_PATH, this::handleEntities);
        server.createContext(IMPORT_PATH, this::handleImport);
        server.createContext(EXPORT_PATH, this::handleExport);
        server.createContext(DRL_PATH, this::handleDrl);
        server.createContext(DRL_SAMPLE_PATH, this::handleDrlSample);
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "GET");
            return;
        }
        writeJson(exchange, 200, Map.of(
                "status", "UP",
                "service", "coredeux-java-native-demo",
                "port", port()));
    }

    private void handleOpenApiDocs(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "GET");
            return;
        }
        writeJson(exchange, 200, openApiDocument());
    }

    private void handleSwaggerUi(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "GET");
            return;
        }
        writeHtml(exchange, 200, swaggerUiHtml());
    }

    private void handleEntities(HttpExchange exchange) throws IOException {
        String tail = trimLeadingSlash(tail(exchange, ENTITY_PATH));
        if (tail.isBlank()) {
            sendError(exchange, 400, "Entity name must not be blank");
            return;
        }
        int slash = tail.indexOf('/');
        String entityName = slash < 0 ? tail : tail.substring(0, slash);
        String remainder = slash < 0 ? "" : tail.substring(slash + 1);
        handleEntity(exchange, entityName, remainder);
    }

    private void handleEntity(HttpExchange exchange, String entityName, String tail) throws IOException {
        String normalizedTail = trimLeadingSlash(tail);
        if (normalizedTail.isBlank()) {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                listEntities(exchange, entityName);
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                createEntity(exchange, entityName);
                return;
            }
            methodNotAllowed(exchange, "GET", "POST");
            return;
        }

        String id = normalizedTail;
        if (id.isBlank()) {
            sendError(exchange, 400, "Entity id must not be blank");
            return;
        }

        switch (exchange.getRequestMethod().toUpperCase()) {
            case "GET" -> readEntity(exchange, entityName, id);
            case "PUT" -> updateEntity(exchange, entityName, id);
            case "DELETE" -> deleteEntity(exchange, entityName, id);
            default -> methodNotAllowed(exchange, "GET", "PUT", "DELETE");
        }
    }

    private Map<String, Object> openApiDocument() {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("openapi", "3.0.3");
        document.put("info", Map.of(
                "title", "Coredeux Java Native Demo API",
                "description", "Plain Java demo endpoints for Coredeux Core, import, export, and the embedded HTTP runtime.",
                "version", "0.1.0-SNAPSHOT"));
        document.put("servers", List.of(Map.of(
                "url", "http://localhost:" + port(),
                "description", "Native demo server")));
        document.put("paths", openApiPaths());
        document.put("components", Map.of("schemas", openApiSchemas()));
        return document;
    }

    private Map<String, Object> openApiPaths() {
        Map<String, Object> paths = new LinkedHashMap<>();
        paths.put("/health", Map.of(
                "get", operation(
                        "Check health",
                        "Returns the current native demo status.",
                        null,
                        Map.of("200", jsonResponse("HealthResponse")))));

        paths.put("/api/entities/{entityName}", Map.of(
                "get", operation(
                        "List entities",
                        "Lists managed entities by full class name.",
                        List.of(parameter("entityName", "path", "Full entity class name", true, refSchema("string"))),
                        Map.of(
                                "200", jsonResponse("EntityDocumentArray"),
                                "400", errorResponse(),
                                "404", errorResponse())),
                "post", operation(
                        "Create entity",
                        "Creates a new entity instance for the requested entity type.",
                        List.of(parameter("entityName", "path", "Full entity class name", true, refSchema("string"))),
                        Map.of(
                                "201", jsonResponse("EntityDocument"),
                                "400", errorResponse(),
                                "404", errorResponse()),
                        refSchema("EntityDocument"))));

        paths.put("/api/entities/{entityName}/{id}", Map.of(
                "get", operation(
                        "Read entity",
                        "Reads a single entity by identifier.",
                        List.of(
                                parameter("entityName", "path", "Full entity class name", true, refSchema("string")),
                                parameter("id", "path", "Entity identifier", true, refSchema("string"))),
                        Map.of("200", jsonResponse("EntityDocument"), "404", errorResponse())),
                "put", operation(
                        "Update entity",
                        "Updates an existing entity by identifier.",
                        List.of(
                                parameter("entityName", "path", "Full entity class name", true, refSchema("string")),
                                parameter("id", "path", "Entity identifier", true, refSchema("string"))),
                        Map.of(
                                "200", jsonResponse("EntityDocument"),
                                "400", errorResponse(),
                                "404", errorResponse()),
                        refSchema("EntityDocument")),
                "delete", operation(
                        "Delete entity",
                        "Deletes an entity by identifier.",
                        List.of(
                                parameter("entityName", "path", "Full entity class name", true, refSchema("string")),
                                parameter("id", "path", "Entity identifier", true, refSchema("string"))),
                        Map.of(
                                "204", Map.of("description", "Entity deleted"),
                                "404", errorResponse()))));

        paths.put("/api/import", Map.of(
                "post", operation(
                        "Import data",
                        "Validates and imports a JSON import request.",
                        null,
                        Map.of(
                                "200", jsonResponse("ImportResponse"),
                                "400", errorResponse()),
                        refSchema("ImportRequest"))));

        paths.put("/api/import/validate", Map.of(
                "post", operation(
                        "Validate import",
                        "Validates an import request without executing it.",
                        null,
                        Map.of(
                                "200", jsonResponse("ImportResponse"),
                                "400", errorResponse()),
                        refSchema("ImportRequest"))));

        paths.put("/api/import/sample", Map.of(
                "post", operation(
                        "Import sample file",
                        "Loads the bundled Postgres sample import file and executes it.",
                        null,
                        Map.of(
                                "200", jsonResponse("ImportResponse"),
                                "400", errorResponse()))));

        paths.put("/api/import/file", Map.of(
                "post", operation(
                        "Import file",
                        "Uploads a text or Excel import file and executes it.",
                        List.of(parameter("parser", "query", "Optional parser override", false, refSchema("string"))),
                        Map.of(
                                "200", jsonResponse("ImportResponse"),
                                "400", errorResponse()),
                        refSchema("binary"),
                        "application/octet-stream")));

        paths.put("/api/import/file/validate", Map.of(
                "post", operation(
                        "Validate file import",
                        "Uploads an import file and validates it without executing the import.",
                        List.of(parameter("parser", "query", "Optional parser override", false, refSchema("string"))),
                        Map.of(
                                "200", jsonResponse("ImportResponse"),
                                "400", errorResponse()),
                        refSchema("binary"),
                        "application/octet-stream")));

        paths.put("/api/export", Map.of(
                "post", operation(
                        "Queue export",
                        "Queues an export job and processes it in the demo runtime.",
                        null,
                        Map.of(
                                "200", jsonResponse("ExportResponse"),
                                "202", jsonResponse("ExportResponse"),
                                "400", errorResponse()),
                        refSchema("ExportRequest"))));

        paths.put("/api/export/{uid}", Map.of(
                "get", operation(
                        "Read export",
                        "Fetches the current export job state by uid.",
                        List.of(parameter("uid", "path", "Export uid", true, refSchema("string"))),
                        Map.of(
                                "200", jsonResponse("ExportResponse"),
                                "202", jsonResponse("ExportResponse"),
                                "404", errorResponse()))));

        paths.put("/api/export/{uid}/download", Map.of(
                "get", operation(
                        "Download export",
                        "Downloads the exported artifact once the job is complete.",
                        List.of(parameter("uid", "path", "Export uid", true, refSchema("string"))),
                        Map.of(
                                "200", Map.of("description", "Binary export artifact"),
                                "404", errorResponse(),
                                "409", errorResponse()))));

        paths.put("/api/drl/rules", Map.of(
                "get", operation(
                        "List DRL rules",
                        "Lists the persisted DRL rule sources.",
                        null,
                        Map.of("200", jsonResponse("DrlRuleRecordArray")))));

        paths.put("/api/drl/rules/{ruleId}", Map.of(
                "get", operation(
                        "Read DRL rule",
                        "Reads a single DRL rule source by code.",
                        List.of(parameter("ruleId", "path", "Rule code", true, refSchema("string"))),
                        Map.of("200", jsonResponse("DrlRuleRecord"), "404", errorResponse())),
                "put", operation(
                        "Upsert DRL rule",
                        "Creates or updates a DRL rule source.",
                        List.of(parameter("ruleId", "path", "Rule code", true, refSchema("string"))),
                        Map.of("200", jsonResponse("DrlRuleRecord"), "400", errorResponse()),
                        refSchema("DrlRuleUpsertRequest")),
                "delete", operation(
                        "Delete DRL rule",
                        "Deletes a DRL rule source by code.",
                        List.of(parameter("ruleId", "path", "Rule code", true, refSchema("string"))),
                        Map.of("204", Map.of("description", "Rule deleted"), "404", errorResponse()))));

        paths.put("/api/drl/rules/convert", Map.of(
                "post", operation(
                        "Convert annotated Java to DRL",
                        "Converts annotated Java source into DRL and stores it.",
                        null,
                        Map.of("200", jsonResponse("DrlRuleRecord"), "400", errorResponse()),
                        refSchema("DrlConversionRequest"))));

        paths.put("/api/drl/rules/{ruleId}/execute", Map.of(
                "post", operation(
                        "Execute DRL rule",
                        "Executes a cached or resolver-backed DRL rule by code.",
                        List.of(parameter("ruleId", "path", "Rule code", true, refSchema("string"))),
                        Map.of("200", jsonResponse("RuleContext"), "404", errorResponse()),
                        refSchema("RuleContext"))));

        paths.put("/api/drl/rules/{ruleId}/execute-source", Map.of(
                "post", operation(
                        "Compile, cache, and execute DRL source",
                        "Compiles caller-provided DRL source, caches it by rule code, and executes it.",
                        List.of(parameter("ruleId", "path", "Rule code", true, refSchema("string"))),
                        Map.of("200", jsonResponse("RuleContext"), "400", errorResponse()),
                        refSchema("DrlSourceExecutionRequest"))));

        paths.put("/api/drl/execute-source", Map.of(
                "post", operation(
                        "Compile and execute DRL source",
                        "Compiles and executes caller-provided DRL source without caching it.",
                        null,
                        Map.of("200", jsonResponse("RuleContext"), "400", errorResponse()),
                        refSchema("DrlSourceExecutionRequest"))));

        paths.put("/api/drl/cache", Map.of(
                "delete", operation(
                        "Clear DRL cache",
                        "Removes all cached compiled DRL rule bases.",
                        null,
                        Map.of("204", Map.of("description", "Cache cleared")))));

        paths.put("/api/drl/cache/{ruleId}", Map.of(
                "get", operation(
                        "Check DRL cache",
                        "Checks whether a rule code is currently cached.",
                        List.of(parameter("ruleId", "path", "Rule code", true, refSchema("string"))),
                        Map.of("200", jsonResponse("CacheStatus"), "404", errorResponse())),
                "delete", operation(
                        "Remove DRL cache entry",
                        "Removes one cached DRL rule base.",
                        List.of(parameter("ruleId", "path", "Rule code", true, refSchema("string"))),
                        Map.of("204", Map.of("description", "Cache entry removed")))));

        paths.put("/api/drl/sample/greet", Map.of(
                "post", operation(
                        "Execute sample greet rule",
                        "Executes the seeded sample rule using the greet method and returns the populated RuleContext.",
                        null,
                        Map.of("200", jsonResponse("RuleContext"), "400", errorResponse()),
                        refSchema("RuleContext"))));

        paths.put("/api/drl/sample/count-facts", Map.of(
                "post", operation(
                        "Execute sample count facts rule",
                        "Executes the seeded sample rule using the countFacts method and returns the populated RuleContext.",
                        null,
                        Map.of("200", jsonResponse("RuleContext"), "400", errorResponse()),
                        refSchema("RuleContext"))));

        return paths;
    }

    private Map<String, Object> openApiSchemas() {
        Map<String, Object> schemas = new LinkedHashMap<>();
        schemas.put("string", Map.of("type", "string"));
        schemas.put("binary", Map.of("type", "string", "format", "binary"));
        schemas.put("HealthResponse", objectSchema(Map.of(
                "status", Map.of("type", "string"),
                "service", Map.of("type", "string"),
                "port", Map.of("type", "integer", "format", "int32")),
                List.of("status", "service", "port")));
        schemas.put("EntityDocument", objectSchema(Map.of(), List.of()));
        schemas.put("EntityDocumentArray", Map.of(
                "type", "array",
                "items", Map.of("$ref", "#/components/schemas/EntityDocument")));
        schemas.put("ImportRequest", objectSchema(Map.of(), List.of()));
        schemas.put("ImportResponse", objectSchema(Map.of(), List.of()));
        schemas.put("ExportRequest", objectSchema(Map.of(), List.of()));
        schemas.put("ExportResponse", objectSchema(Map.of(), List.of()));
        schemas.put("DrlRuleRecord", objectSchema(Map.of(
                "id", Map.of("type", "integer", "format", "int64"),
                "code", Map.of("type", "string"),
                "description", Map.of("type", "string"),
                "drl", Map.of("type", "string")), List.of("code", "drl")));
        schemas.put("DrlRuleRecordArray", Map.of(
                "type", "array",
                "items", Map.of("$ref", "#/components/schemas/DrlRuleRecord")));
        schemas.put("DrlRuleUpsertRequest", objectSchema(Map.of(
                "description", Map.of("type", "string"),
                "drl", Map.of("type", "string")), List.of("drl")));
        schemas.put("DrlConversionRequest", objectSchema(Map.of(
                "source", Map.of("type", "string")), List.of("source")));
        schemas.put("DrlSourceExecutionRequest", objectSchema(Map.of(
                "source", Map.of("type", "string"),
                "context", Map.of("$ref", "#/components/schemas/RuleContext")), List.of("source", "context")));
        schemas.put("RuleContext", objectSchema(Map.of(
                "method", Map.of("type", "string"),
                "params", Map.of("type", "object", "additionalProperties", Map.of("type", "object")),
                "facts", Map.of("type", "array", "items", Map.of("type", "object")),
                "output", Map.of(),
                "message", Map.of("type", "string"),
                "exception", Map.of(),
                "firedRules", Map.of("type", "integer", "format", "int32")), List.of()));
        schemas.put("CacheStatus", objectSchema(Map.of(
                "ruleId", Map.of("type", "string"),
                "cached", Map.of("type", "boolean")), List.of("ruleId", "cached")));
        return schemas;
    }

    private Map<String, Object> operation(String summary, String description, List<Map<String, Object>> parameters,
            Map<String, Object> responses) {
        return operation(summary, description, parameters, responses, null);
    }

    private Map<String, Object> operation(String summary, String description, List<Map<String, Object>> parameters,
            Map<String, Object> responses, Map<String, Object> requestBodySchema, String requestBodyContentType) {
        Map<String, Object> operation = operation(summary, description, parameters, responses, requestBodySchema);
        if (requestBodySchema != null && requestBodyContentType != null && !requestBodyContentType.isBlank()) {
            operation.put("requestBody", Map.of(
                    "required", true,
                    "content", Map.of(requestBodyContentType, Map.of("schema", requestBodySchema))));
        }
        return operation;
    }

    private Map<String, Object> operation(String summary, String description, List<Map<String, Object>> parameters,
            Map<String, Object> responses, Map<String, Object> requestBodySchema) {
        Map<String, Object> operation = new LinkedHashMap<>();
        operation.put("summary", summary);
        operation.put("description", description);
        operation.put("responses", responses);
        if (parameters != null && !parameters.isEmpty()) {
            operation.put("parameters", parameters);
        }
        if (requestBodySchema != null) {
            operation.put("requestBody", Map.of(
                    "required", true,
                    "content", Map.of("application/json", Map.of("schema", requestBodySchema))));
        }
        return operation;
    }

    private Map<String, Object> parameter(String name, String in, String description, boolean required,
            Map<String, Object> schema) {
        Map<String, Object> parameter = new LinkedHashMap<>();
        parameter.put("name", name);
        parameter.put("in", in);
        parameter.put("description", description);
        parameter.put("required", required);
        parameter.put("schema", schema);
        return parameter;
    }

    private Map<String, Object> jsonResponse(String schemaName) {
        return Map.of(
                "description", "OK",
                "content", Map.of(
                        "application/json", Map.of(
                                "schema", Map.of("$ref", "#/components/schemas/" + schemaName))));
    }

    private Map<String, Object> errorResponse() {
        return Map.of(
                "description", "Error",
                "content", Map.of(
                        "application/json", Map.of(
                                "schema", Map.of("$ref", "#/components/schemas/ErrorResponse"))));
    }

    private Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", true);
        if (properties != null && !properties.isEmpty()) {
            schema.put("properties", properties);
        }
        if (required != null && !required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private Map<String, Object> refSchema(String name) {
        return Map.of("$ref", "#/components/schemas/" + name);
    }

    private String swaggerUiHtml() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <title>Coredeux Java Native Demo API</title>
                  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui.css">
                  <style>
                    body { margin: 0; background: #fafafa; }
                    #swagger-ui { max-width: 100%; }
                  </style>
                </head>
                <body>
                  <div id="swagger-ui"></div>
                  <script src="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
                  <script src="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui-standalone-preset.js"></script>
                  <script>
                    window.onload = function() {
                      window.ui = SwaggerUIBundle({
                        url: '/v3/api-docs',
                        dom_id: '#swagger-ui',
                        deepLinking: true,
                        presets: [
                          SwaggerUIBundle.presets.apis,
                          SwaggerUIStandalonePreset
                        ],
                        layout: 'BaseLayout'
                      });
                    };
                  </script>
                </body>
                </html>
                """;
    }

    private void listEntities(HttpExchange exchange, String entityName) throws IOException {
        Map<String, String> query = queryParams(exchange.getRequestURI());
        int pageSize = intValue(query.get("pageSize"), 20);
        int currentPage = intValue(query.get("currentPage"), 1);
        String search = query.get("name");
        Class<?> entityType = entityResolver.resolveType(entityName);
        SearchResult<?> result;
        if (search != null && !search.isBlank()) {
            result = runtime.coredeuxService().loadAll(List.of(
                    SearchParams.builder().field("name").comparator("ANYWHERE").value(search).build()),
                    entityType, pageSize, currentPage);
        } else {
            result = runtime.coredeuxService().loadAll(List.of(), entityType, pageSize, currentPage);
        }
        writeJson(exchange, 200, result);
    }

    private void createEntity(HttpExchange exchange, String entityName) throws IOException {
        try {
            Class<?> entityType = entityResolver.resolveType(entityName);
            CoredeuxEntityDefinition definition = entityResolver.resolveDefinition(entityName);
            Object entity = readJson(exchange, entityType);
            Object identifierValue = entityResolver.getIdentifierValue(entity, definition);
            if (identifierValue == null || identifierValue.toString().isBlank()) {
                Class<?> identifierType = entityResolver.resolveIdentifierType(entityType, definition);
                if (String.class.equals(identifierType)) {
                    entityResolver.applyIdentifier(entity, definition, CoredeuxNativeRuntime.customerId("NT"));
                }
            }
            String id = runtime.coredeuxService().save(entity);
            writeJson(exchange, 201, runtime.coredeuxService().load(id, entityType));
        } catch (CoredeuxValidationException exception) {
            sendValidationErrors(exchange, exception);
        }
    }

    private void readEntity(HttpExchange exchange, String entityName, String id) throws IOException {
        Class<?> entityType = entityResolver.resolveType(entityName);
        Object entity = runtime.coredeuxService().load(id, entityType);
        if (entity == null) {
            sendError(exchange, 404, "Entity not found: " + id);
            return;
        }
        writeJson(exchange, 200, entity);
    }

    private void updateEntity(HttpExchange exchange, String entityName, String id) throws IOException {
        try {
            Class<?> entityType = entityResolver.resolveType(entityName);
            CoredeuxEntityDefinition definition = entityResolver.resolveDefinition(entityName);
            Object entity = readJson(exchange, entityType);
            Class<?> identifierType = entityResolver.resolveIdentifierType(entityType, definition);
            Object typedIdentifier = objectMapper.convertValue(id, identifierType);
            entityResolver.applyIdentifier(entity, definition, typedIdentifier);
            runtime.coredeuxService().update(entity);
            writeJson(exchange, 200, runtime.coredeuxService().load(id, entityType));
        } catch (CoredeuxValidationException exception) {
            sendValidationErrors(exchange, exception);
        }
    }

    private void deleteEntity(HttpExchange exchange, String entityName, String id) throws IOException {
        Class<?> entityType = entityResolver.resolveType(entityName);
        runtime.coredeuxService().remove(id, entityType);
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private void handleImport(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Import endpoints require POST");
            return;
        }

        if (path.equals(IMPORT_PATH) || path.equals(IMPORT_PATH + "/")) {
            ImportRequest request = readJson(exchange, ImportRequest.class);
            ImportResponse response = runtime.coredeuxImportService().importData(request);
            writeJson(exchange, response.hasErrors() ? 400 : 200, response);
            return;
        }

        if (path.equals(IMPORT_PATH + "/validate")) {
            ImportRequest request = readJson(exchange, ImportRequest.class);
            ImportResponse response = runtime.coredeuxImportService().validateData(request);
            writeJson(exchange, response.hasErrors() ? 400 : 200, response);
            return;
        }

        if (path.equals(IMPORT_PATH + "/sample")) {
            ImportRequest request = parseSampleImportRequest();
            ImportResponse response = runtime.coredeuxImportService().importData(request);
            writeJson(exchange, response.hasErrors() ? 400 : 200, response);
            return;
        }

        if (path.equals(IMPORT_PATH + "/file/validate")) {
            byte[] body = exchange.getRequestBody().readAllBytes();
            ImportRequest request = parseImportFile(exchange, body);
            ImportResponse response = runtime.coredeuxImportService().validateData(request);
            writeJson(exchange, response.hasErrors() ? 400 : 200, response);
            return;
        }

        if (path.equals(IMPORT_PATH + "/file")) {
            byte[] body = exchange.getRequestBody().readAllBytes();
            ImportRequest request = parseImportFile(exchange, body);
            ImportResponse response = runtime.coredeuxImportService().importData(request);
            writeJson(exchange, response.hasErrors() ? 400 : 200, response);
            return;
        }

        sendError(exchange, 404, "Unknown import endpoint: " + path);
    }

    private void handleExport(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals(EXPORT_PATH) || path.equals(EXPORT_PATH + "/")) {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                methodNotAllowed(exchange, "POST");
                return;
            }
            ExportRequest request = readJson(exchange, ExportRequest.class);
            ExportResponse response = runtime.coredeuxExportService().queueExport(request);
            runtime.processPendingExportJobs();
            writeJson(exchange, exportStatusCode(response), response);
            return;
        }

        String tail = path.substring(EXPORT_PATH.length());
        if (tail.startsWith("/")) {
            tail = tail.substring(1);
        }
        if (tail.isBlank()) {
            sendError(exchange, 404, "Unknown export endpoint: " + path);
            return;
        }

        String uid = tail;
        String suffix = "";
        int slash = tail.indexOf('/');
        if (slash >= 0) {
            uid = tail.substring(0, slash);
            suffix = tail.substring(slash + 1);
        }

        ExportResponse response = runtime.coredeuxExportService().getExport(uid);
        if (suffix.isBlank()) {
            writeJson(exchange, exportStatusCode(response), response);
            return;
        }

        if (!"download".equalsIgnoreCase(suffix)) {
            sendError(exchange, 404, "Unknown export endpoint: " + path);
            return;
        }
        downloadExport(exchange, response);
    }

    private void handleDrl(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        try {
            if (path.equals(DRL_PATH + "/rules") || path.equals(DRL_PATH + "/rules/")) {
                handleDrlRules(exchange);
                return;
            }
            if (path.equals(DRL_PATH + "/rules/convert")) {
                handleDrlConvert(exchange);
                return;
            }
            if (path.startsWith(DRL_PATH + "/rules/")) {
                handleDrlRule(exchange, path.substring((DRL_PATH + "/rules/").length()));
                return;
            }
            if (path.equals(DRL_PATH + "/execute-source")) {
                handleDrlExecuteSource(exchange);
                return;
            }
            if (path.equals(DRL_PATH + "/cache") || path.equals(DRL_PATH + "/cache/")) {
                handleDrlCache(exchange);
                return;
            }
            if (path.startsWith(DRL_PATH + "/cache/")) {
                handleDrlCacheRule(exchange, path.substring((DRL_PATH + "/cache/").length()));
                return;
            }
            sendError(exchange, 404, "Unknown DRL endpoint: " + path);
        } catch (DrlRuleNotFoundException exception) {
            sendError(exchange, 404, exception.getMessage());
        } catch (DrlConversionException exception) {
            sendError(exchange, 400, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            sendError(exchange, 400, exception.getMessage());
        }
    }

    private void handleDrlRules(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "GET");
            return;
        }
        writeJson(exchange, 200, runtime.drlRuleSourceService().findAll());
    }

    private void handleDrlConvert(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "POST");
            return;
        }
        DrlConversionRequest request = readJson(exchange, DrlConversionRequest.class);
        writeJson(exchange, 200, runtime.drlRuleSourceService().convertAndSave(request.getSource()));
    }

    private void handleDrlRule(HttpExchange exchange, String tail) throws IOException {
        String normalizedTail = trimLeadingSlash(tail);
        if (normalizedTail.isBlank()) {
            sendError(exchange, 400, "Rule code must not be blank");
            return;
        }
        int slash = normalizedTail.indexOf('/');
        String ruleId = slash < 0 ? normalizedTail : normalizedTail.substring(0, slash);
        String remainder = slash < 0 ? "" : normalizedTail.substring(slash + 1);
        if (remainder.isBlank()) {
            switch (exchange.getRequestMethod().toUpperCase()) {
                case "GET" -> readDrlRule(exchange, ruleId);
                case "PUT" -> upsertDrlRule(exchange, ruleId);
                case "DELETE" -> deleteDrlRule(exchange, ruleId);
                default -> methodNotAllowed(exchange, "GET", "PUT", "DELETE");
            }
            return;
        }

        switch (remainder) {
            case "execute" -> executeDrlRule(exchange, ruleId);
            case "execute-source" -> executeDrlRuleWithSource(exchange, ruleId);
            default -> sendError(exchange, 404, "Unknown DRL endpoint: " + DRL_PATH + "/rules/" + ruleId + "/" + remainder);
        }
    }

    private void readDrlRule(HttpExchange exchange, String ruleId) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "GET");
            return;
        }
        writeJson(exchange, 200, runtime.drlRuleSourceService().findByCode(ruleId)
                .orElseThrow(() -> new DrlRuleNotFoundException(ruleId)));
    }

    private void upsertDrlRule(HttpExchange exchange, String ruleId) throws IOException {
        if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "PUT");
            return;
        }
        DrlRuleUpsertRequest request = readJson(exchange, DrlRuleUpsertRequest.class);
        writeJson(exchange, 200, runtime.drlRuleSourceService().save(ruleId,
                request == null ? null : request.getDescription(),
                request == null ? null : request.getDrl()));
    }

    private void deleteDrlRule(HttpExchange exchange, String ruleId) throws IOException {
        if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "DELETE");
            return;
        }
        runtime.drlRuleSourceService().delete(ruleId);
        runtime.drlService().purgeCache(ruleId);
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private void executeDrlRule(HttpExchange exchange, String ruleId) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "POST");
            return;
        }
        RuleContext<?> context = readRuleContext(exchange);
        runtime.drlService().execute(ruleId, context);
        writeJson(exchange, 200, context);
    }

    private void executeDrlRuleWithSource(HttpExchange exchange, String ruleId) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "POST");
            return;
        }
        DrlSourceExecutionRequest request = readJson(exchange, DrlSourceExecutionRequest.class);
        RuleContext<?> context = request == null ? null : request.getContext();
        runtime.drlService().execute(ruleId, request == null ? null : request.getSource(), context);
        writeJson(exchange, 200, context);
    }

    private void handleDrlExecuteSource(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "POST");
            return;
        }
        DrlSourceExecutionRequest request = readJson(exchange, DrlSourceExecutionRequest.class);
        RuleContext<?> context = request == null ? null : request.getContext();
        runtime.drlService().executeSource(request == null ? null : request.getSource(), context);
        writeJson(exchange, 200, context);
    }

    private void handleDrlCache(HttpExchange exchange) throws IOException {
        if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "DELETE");
            return;
        }
        runtime.drlService().purgeCache();
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private void handleDrlCacheRule(HttpExchange exchange, String tail) throws IOException {
        String ruleId = trimLeadingSlash(tail);
        if (ruleId.isBlank()) {
            sendError(exchange, 400, "Rule code must not be blank");
            return;
        }
        switch (exchange.getRequestMethod().toUpperCase()) {
            case "GET" -> writeJson(exchange, 200, Map.of(
                    "ruleId", ruleId,
                    "cached", runtime.drlService().isCached(ruleId)));
            case "DELETE" -> {
                runtime.drlService().purgeCache(ruleId);
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
            }
            default -> methodNotAllowed(exchange, "GET", "DELETE");
        }
    }

    private void handleDrlSample(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        try {
            if (path.equals(DRL_SAMPLE_PATH + "/greet")) {
                executeGreetingSampleDrlRule(exchange);
                return;
            }
            if (path.equals(DRL_SAMPLE_PATH + "/count-facts")) {
                executeCountFactsSampleDrlRule(exchange);
                return;
            }
            sendError(exchange, 404, "Unknown DRL sample endpoint: " + path);
        } catch (DrlRuleNotFoundException exception) {
            sendError(exchange, 404, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            sendError(exchange, 400, exception.getMessage());
        }
    }

    private void executeGreetingSampleDrlRule(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "POST");
            return;
        }
        RuleContext<String> context = readGreetingRuleContextOrNull(exchange);
        if (context == null) {
            context = RuleContext.method("greet");
        } else if (context.getMethod() == null || context.getMethod().isBlank()) {
            context.setMethod("greet");
        } else {
            context.setMethod("greet");
        }
        runtime.drlService().execute("demoGreetingRuleSource", context);
        writeJson(exchange, 200, context);
    }

    private void executeCountFactsSampleDrlRule(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "POST");
            return;
        }
        RuleContext<Integer> context = readCountFactsRuleContextOrNull(exchange);
        if (context == null) {
            context = RuleContext.method("countFacts");
        } else if (context.getMethod() == null || context.getMethod().isBlank()) {
            context.setMethod("countFacts");
        } else {
            context.setMethod("countFacts");
        }
        runtime.drlService().execute("demoGreetingRuleSource", context);
        writeJson(exchange, 200, context);
    }

    private void downloadExport(HttpExchange exchange, ExportResponse response) throws IOException {
        if (response == null || response.getStorage() == null
                || response.getStorage().getAbsolutePath() == null
                || response.getStorage().getAbsolutePath().isBlank()) {
            writeJson(exchange, 409, Map.of(
                    "message", "Export file is not ready yet",
                    "status", response == null ? null : response.getStatus()));
            return;
        }
        Path file = Path.of(response.getStorage().getAbsolutePath());
        if (!Files.exists(file)) {
            sendError(exchange, 404, "Export file not found: " + file);
            return;
        }
        byte[] bytes = Files.readAllBytes(file);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", response.getContentType() == null ? "application/octet-stream"
                : response.getContentType());
        headers.set("Content-Disposition", "attachment; filename=\""
                + (response.getFileName() == null ? file.getFileName().toString() : response.getFileName()) + "\"");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private int exportStatusCode(ExportResponse response) {
        if (response == null || response.getStatus() == null) {
            return 200;
        }
        return ExportStatus.NEW.equals(response.getStatus()) || ExportStatus.IN_PROGRESS.equals(response.getStatus())
                ? 202
                : 200;
    }

    private ImportRequest parseSampleImportRequest() throws IOException {
        try (InputStream inputStream = CoredeuxNativeDemoServer.class.getClassLoader()
                .getResourceAsStream(SAMPLE_IMPORT_PATH)) {
            if (inputStream == null) {
                throw new IOException("Missing sample import file: " + SAMPLE_IMPORT_PATH);
            }
            String parser = defaultImportParser();
            return parseImportBytes(inputStream.readAllBytes(), parser, "text/plain");
        }
    }

    private ImportRequest parseImportFile(HttpExchange exchange, byte[] body) throws IOException {
        Map<String, String> query = queryParams(exchange.getRequestURI());
        String parser = query.getOrDefault("parser", defaultImportParser());
        String contentType = Optional.ofNullable(exchange.getRequestHeaders().getFirst("Content-Type"))
                .orElse("text/plain");
        return parseImportBytes(body, parser, contentType);
    }

    private ImportRequest parseImportBytes(byte[] body, String parser, String contentType) {
        String normalized = parser == null ? "" : parser.trim().toLowerCase();
        if ("excel".equals(normalized) || contentType.toLowerCase().contains("spreadsheet")
                || contentType.toLowerCase().contains("excel")) {
            return new CoredeuxExcelImportParser().parse(new ByteArrayInputStream(body));
        }
        return new CoredeuxTextImportParser().parse(new String(body, StandardCharsets.UTF_8));
    }

    private String defaultImportParser() {
        String parser = runtime.coredeuxProperties().string("import.default-parser");
        return parser == null || parser.isBlank() ? "text" : parser.trim();
    }

    private <T> T readJson(HttpExchange exchange, Class<T> type) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            throw new IOException("Request body must not be empty");
        }
        return objectMapper.readValue(body, type);
    }

    private <T> T readJsonOrNull(HttpExchange exchange, Class<T> type) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            return null;
        }
        return objectMapper.readValue(body, type);
    }

    private RuleContext<?> readRuleContext(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            throw new IOException("Request body must not be empty");
        }
        return objectMapper.readValue(body, new TypeReference<RuleContext<?>>() {
        });
    }

    private RuleContext<?> readRuleContextOrNull(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            return null;
        }
        return objectMapper.readValue(body, new TypeReference<RuleContext<?>>() {
        });
    }

    private RuleContext<String> readGreetingRuleContextOrNull(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            return null;
        }
        return objectMapper.readValue(body, new TypeReference<RuleContext<String>>() {
        });
    }

    private RuleContext<Integer> readCountFactsRuleContextOrNull(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            return null;
        }
        return objectMapper.readValue(body, new TypeReference<RuleContext<Integer>>() {
        });
    }

    private void sendValidationErrors(HttpExchange exchange, CoredeuxValidationException exception) throws IOException {
        List<Map<String, Object>> errors = exception.getValidationErrors().stream()
                .map(error -> Map.<String, Object>of(
                        "field", error.getField(),
                        "message", error.getMessage()))
                .toList();
        writeJson(exchange, 400, Map.of(
                "message", exception.getMessage(),
                "validationErrors", errors));
    }

    private void methodNotAllowed(HttpExchange exchange, String... allowed) throws IOException {
        exchange.getResponseHeaders().set("Allow", String.join(", ", allowed));
        sendError(exchange, 405, "Method not allowed");
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        writeJson(exchange, status, Map.of("message", message));
    }

    private void writeHtml(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private void writeJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private Map<String, String> queryParams(URI uri) {
        Map<String, String> params = new LinkedHashMap<>();
        String query = uri == null ? null : uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return params;
        }
        for (String pair : query.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            int index = pair.indexOf('=');
            String key = index < 0 ? pair : pair.substring(0, index);
            String value = index < 0 ? "" : pair.substring(index + 1);
            params.put(decode(key), decode(value));
        }
        return params;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private int intValue(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private String tail(HttpExchange exchange, String basePath) {
        String path = exchange.getRequestURI().getPath();
        return path.length() <= basePath.length() ? "" : path.substring(basePath.length());
    }

    private String trimLeadingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.startsWith("/") ? value.substring(1) : value;
    }
}
