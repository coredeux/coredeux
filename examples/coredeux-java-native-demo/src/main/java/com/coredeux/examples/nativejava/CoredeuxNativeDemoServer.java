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
import com.coredeux.export.model.ExportFormat;
import com.coredeux.export.model.ExportRequest;
import com.coredeux.export.model.ExportResponse;
import com.coredeux.export.model.ExportStatus;
import com.coredeux.export.service.CoredeuxExportService;
import com.coredeux.export.support.ExportJsonSupport;
import com.coredeux.demo.domain.Customer;
import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.model.ImportResponse;
import com.coredeux.impex.parser.excel.CoredeuxExcelImportParser;
import com.coredeux.impex.parser.text.CoredeuxTextImportParser;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final String CUSTOMER_PATH = "/api/customers";
    private static final String IMPORT_PATH = "/api/import";
    private static final String EXPORT_PATH = "/api/export";
    private static final String SAMPLE_IMPORT_PATH = "samples/postgres-customers.import";
    private static final String CUSTOMER_ENTITY_NAME = Customer.class.getName();

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
                new NativeEntityResolver(runtime.entityDefinitionRegistry(), runtime.reflectionHelperService()),
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
        server.createContext(ENTITY_PATH, this::handleEntities);
        server.createContext(CUSTOMER_PATH, this::handleCustomers);
        server.createContext(IMPORT_PATH, this::handleImport);
        server.createContext(EXPORT_PATH, this::handleExport);
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

    private void handleCustomers(HttpExchange exchange) throws IOException {
        handleEntity(exchange, CUSTOMER_ENTITY_NAME, tail(exchange, CUSTOMER_PATH));
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
