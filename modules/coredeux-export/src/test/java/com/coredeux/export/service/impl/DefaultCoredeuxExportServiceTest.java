package com.coredeux.export.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.core.registry.InMemoryEntityDefinitionRegistry;
import com.coredeux.core.search.PaginationData;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxService;
import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.log.CoredeuxExportLogService;
import com.coredeux.export.log.CoredeuxExportLogServiceResolver;
import com.coredeux.export.model.ExportField;
import com.coredeux.export.model.ExportFormat;
import com.coredeux.export.model.ExportJob;
import com.coredeux.export.model.ExportLogEntry;
import com.coredeux.export.model.ExportOptions;
import com.coredeux.export.model.ExportQuery;
import com.coredeux.export.model.ExportRequest;
import com.coredeux.export.model.ExportResponse;
import com.coredeux.export.model.ExportStatus;
import com.coredeux.export.model.ExportStorageArtifact;
import com.coredeux.export.queue.CoredeuxExportQueueService;
import com.coredeux.export.service.CoredeuxExportExecutionService;
import com.coredeux.export.storage.CoredeuxExportStorageService;
import com.coredeux.export.storage.CoredeuxExportStorageServiceResolver;
import com.coredeux.export.log.impl.DefaultCoredeuxExportLogServiceResolver;
import com.coredeux.export.storage.impl.DefaultCoredeuxExportStorageServiceResolver;
import com.coredeux.export.storage.impl.DefaultCoredeuxFileSystemExportStorageService;
import com.coredeux.export.handler.ExportValueHandlerResolver;
import com.coredeux.export.writer.ExcelExportWriter;
import com.coredeux.export.writer.TextExportWriter;
import com.coredeux.export.worker.DefaultCoredeuxExportWorker;

class DefaultCoredeuxExportServiceTest {

    private FakeCoredeuxService coredeuxService;
    private InMemoryQueueService queueService;
    private InMemoryLogService logService;
    private DefaultCoredeuxExportService exportService;
    private DefaultCoredeuxExportWorker worker;

    @BeforeEach
    void setUp() {
        coredeuxService = new FakeCoredeuxService();
        queueService = new InMemoryQueueService();
        logService = new InMemoryLogService();

        DefaultCoredeuxReflectionHelperService reflection = new DefaultCoredeuxReflectionHelperService();
        InMemoryEntityDefinitionRegistry registry = new InMemoryEntityDefinitionRegistry(List.of(
                CoredeuxEntityDefinition.builder().fullClassName(Customer.class.getName()).identifier("id").build()));
        ExportValueFormatter formatter = new ExportValueFormatter();
        ExportValueHandlerResolver handlerResolver = new ExportValueHandlerResolver(Map.of(
                "defaultCoredeuxExportValueHandler", new com.coredeux.export.handler.impl.DefaultCoredeuxExportValueHandler(),
                "dateOnlyExportHandler", new DateOnlyExportHandler()));

        Map<String, CoredeuxExportStorageService> storageServices = new LinkedHashMap<>();
        storageServices.put("defaultCoredeuxExportStorageService",
                new DefaultCoredeuxFileSystemExportStorageService(testBaseDirectory()));
        storageServices.put("customStorageService", new RecordingStorageService("customStorageService"));

        CoredeuxExportStorageServiceResolver storageResolver = new DefaultCoredeuxExportStorageServiceResolver(
                storageServices, "defaultCoredeuxExportStorageService");
        CoredeuxExportLogServiceResolver logResolver = new DefaultCoredeuxExportLogServiceResolver(Map.of(
                "defaultCoredeuxExportLogService", new com.coredeux.export.log.impl.FileCoredeuxExportLogService(),
                "consoleCoredeuxExportLogService", new com.coredeux.export.log.impl.ConsoleCoredeuxExportLogService()),
                "defaultCoredeuxExportLogService");

        exportService = new DefaultCoredeuxExportService(coredeuxService, reflection, registry,
                new ExportFieldPathParser(), new ExportValueResolver(reflection, formatter, handlerResolver),
                storageResolver, logResolver, queueService, new TextExportWriter(), new ExcelExportWriter());

        worker = new DefaultCoredeuxExportWorker(queueService, exportService, logResolver, 1, true);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (worker != null) {
            worker.shutdown();
        }
    }

    @Test
    void queueExportReturnsPendingStatusAndUid() {
        ExportResponse response = exportService.queueExport(baseRequest());

        assertNotNull(response.getUid());
        assertEquals(ExportStatus.NEW, response.getStatus());
        assertFalse(response.getLogs().isEmpty());
        assertEquals("Export queued", response.getLogs().get(0).getMessage());
        assertEquals(ExportStatus.NEW, exportService.getExport(response.getUid()).getStatus());
    }

    @Test
    void executeExportUsesSearchParamsAndFlattensNestedFields() throws Exception {
        coredeuxService.results = List.of(sampleCustomer());
        ExportRequest request = baseRequest();
        request.setSearchParams(List.of(new SearchParams("active", "EQUALS", true)));

        ExportJob job = queueService.enqueue(request);
        queueService.claimNext(1);
        ExportResponse response = exportService.execute(job.getUid(), job.getRequest());

        assertEquals(ExportStatus.COMPLETED, response.getStatus());
        assertTrue(storedText(response).contains("name|email|active|profile:biography|profile:legacySignupDate|phoneNumbers|roles:code"));
        assertTrue(storedText(response).contains("Jane|jane@example.com|true|Bio|2026-05-04T00:00:00Z|111, 222|ADMIN, USER"));
        assertEquals(1, response.getRowCount());
        assertEquals(1, coredeuxService.lastSearchParams.size());
        assertTrue(exportService.getExport(job.getUid()).getLogs().stream().anyMatch(log -> "Export completed".equals(log.getMessage())));
    }

    @Test
    void executeExportUsesQueryWithParams() {
        coredeuxService.results = List.of(sampleCustomer());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("active", true);

        ExportRequest request = baseRequest();
        request.setQuery(ExportQuery.builder()
                .text("select c from Customer c where c.active = :active")
                .params(params)
                .build());

        ExportJob job = queueService.enqueue(request);
        queueService.claimNext(1);
        exportService.execute(job.getUid(), job.getRequest());

        assertEquals("select c from Customer c where c.active = :active", coredeuxService.lastQuery);
        assertEquals(params, coredeuxService.lastQueryParams);
    }

    @Test
    void executeExportUsesSelfContainedQueryWithoutParams() {
        coredeuxService.results = List.of(sampleCustomer());

        ExportRequest request = baseRequest();
        request.setQuery(ExportQuery.builder()
                .text("select c from Customer c where c.active = true")
                .build());

        ExportJob job = queueService.enqueue(request);
        queueService.claimNext(1);
        exportService.execute(job.getUid(), job.getRequest());

        assertEquals("select c from Customer c where c.active = true", coredeuxService.lastQuery);
        assertEquals(Map.of(), coredeuxService.lastQueryParams);
    }

    @Test
    void executeExportRejectsBothQueryStrategies() {
        ExportRequest request = baseRequest();
        request.setSearchParams(List.of(new SearchParams("active", "EQUALS", true)));
        request.setQuery(ExportQuery.builder().text("select c from Customer c").build());

        CoredeuxExportException exception = assertThrows(CoredeuxExportException.class,
                () -> exportService.queueExport(request));
        assertTrue(exception.getMessage().contains("Use either searchParams or query"));
    }

    @Test
    void executeExportRejectsMissingFieldPath() {
        ExportRequest request = ExportRequest.builder()
                .entity(Customer.class.getName())
                .fieldList(List.of(ExportField.builder().path("profile:missing").build()))
                .build();

        assertThrows(CoredeuxExportException.class, () -> exportService.queueExport(request));
    }

    @Test
    void executeExportHonorsLimitAndBatches() {
        coredeuxService.results = List.of(sampleCustomer(), sampleCustomer(), sampleCustomer());
        ExportRequest request = baseRequest();
        request.setOptions(ExportOptions.builder().limit(2).batchSize(1).build());

        ExportJob job = queueService.enqueue(request);
        queueService.claimNext(1);
        ExportResponse response = exportService.execute(job.getUid(), job.getRequest());

        assertEquals(2, response.getRowCount());
        assertEquals(2, coredeuxService.loadAllCalls);
    }

    @Test
    void executeExportQuotesTextCellsContainingSeparatorQuoteOrNewline() throws Exception {
        Customer customer = sampleCustomer();
        customer.setName("Jane | \"JJ\"\nDemo");
        coredeuxService.results = List.of(customer);

        ExportJob job = queueService.enqueue(baseRequest());
        queueService.claimNext(1);
        ExportResponse response = exportService.execute(job.getUid(), job.getRequest());

        String text = storedText(response);
        assertTrue(text.contains("\"Jane | \"\"JJ\"\""));
    }

    @Test
    void executeExportCreatesXlsxContent() throws Exception {
        coredeuxService.results = List.of(sampleCustomer());
        ExportRequest request = baseRequest();
        request.setOptions(ExportOptions.builder().format(ExportFormat.XLSX).build());

        ExportJob job = queueService.enqueue(request);
        queueService.claimNext(1);
        ExportResponse response = exportService.execute(job.getUid(), job.getRequest());

        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", response.getContentType());
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(
                Files.readAllBytes(Path.of(response.getStorage().getAbsolutePath()))))) {
            assertEquals("name", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("Jane", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            assertEquals("jane@example.com", workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
        }
    }

    @Test
    void executeExportResolvesStorageServiceAtRuntime() {
        coredeuxService.results = List.of(sampleCustomer());
        ExportRequest request = baseRequest();
        request.getOptions().setStorageService("customStorageService");

        ExportJob job = queueService.enqueue(request);
        queueService.claimNext(1);
        ExportResponse response = exportService.execute(job.getUid(), job.getRequest());

        assertEquals("customStorageService", response.getStorage().getMetadata().get("service"));
        assertEquals("custom-storage", response.getStorage().getStorageType());
    }

    @Test
    void workerProcessesQueuedJobInBackground() throws Exception {
        coredeuxService.results = List.of(sampleCustomer());
        ExportJob job = queueService.enqueue(baseRequest());

        worker.processPendingExports();
        awaitCompletion(job.getUid());

        ExportResponse response = exportService.getExport(job.getUid());
        assertEquals(ExportStatus.COMPLETED, response.getStatus());
        assertTrue(response.getRowCount() >= 1);
    }

    private void awaitCompletion(String uid) throws InterruptedException {
        for (int attempt = 0; attempt < 50; attempt++) {
            ExportResponse response = exportService.getExport(uid);
            if (ExportStatus.COMPLETED.equals(response.getStatus()) || ExportStatus.ERROR.equals(response.getStatus())) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Timed out waiting for export completion");
    }

    private ExportRequest baseRequest() {
        return ExportRequest.builder()
                .entity(Customer.class.getName())
                .fieldList(fields("name", "email", "active", "profile:biography", "profile:legacySignupDate",
                        "phoneNumbers", "roles:code"))
                .build();
    }

    private Customer sampleCustomer() {
        CustomerProfile profile = new CustomerProfile();
        profile.setBiography("Bio");
        profile.setLegacySignupDate(java.util.Date.from(Instant.parse("2026-05-04T00:00:00Z")));
        Customer customer = new Customer();
        customer.setName("Jane");
        customer.setEmail("jane@example.com");
        customer.setActive(true);
        customer.setCreditLimit(new BigDecimal("19.90"));
        customer.setProfile(profile);
        customer.getPhoneNumbers().addAll(List.of("111", "222"));
        customer.getRoles().addAll(List.of(new Role("ADMIN"), new Role("USER")));
        return customer;
    }

    private List<ExportField> fields(String... paths) {
        return java.util.Arrays.stream(paths)
                .map(path -> ExportField.builder().path(path).build())
                .toList();
    }

    private String storedText(ExportResponse response) throws Exception {
        return Files.readString(Path.of(response.getStorage().getAbsolutePath()));
    }

    private String testBaseDirectory() {
        return Path.of(System.getProperty("java.io.tmpdir"), "coredeux-export-test").toString();
    }

    static class FakeCoredeuxService implements CoredeuxService {

        List<?> results = List.of();
        List<SearchParams> lastSearchParams;
        String lastQuery;
        Map<String, Object> lastQueryParams;
        int loadAllCalls;

        @Override
        public <T> T load(String id, Class<T> type) {
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
                int currentPage) {
            lastQuery = query;
            lastQueryParams = params;
            return resultPage(pageSize, currentPage);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
            loadAllCalls++;
            lastSearchParams = params;
            return resultPage(pageSize, currentPage);
        }

        private <T> SearchResult<T> resultPage(int pageSize, int currentPage) {
            int from = Math.max(currentPage - 1, 0) * pageSize;
            int to = Math.min(from + pageSize, results.size());
            List<T> page = from >= results.size() ? List.of() : (List<T>) results.subList(from, to);
            return SearchResult.<T>builder()
                    .results(page)
                    .pagination(PaginationData.builder()
                            .currentPage((long) currentPage)
                            .pageSize((long) pageSize)
                            .totalResults((long) results.size())
                            .totalPages((long) Math.ceil(results.size() / (double) pageSize))
                            .resultSize((long) page.size())
                            .build())
                    .build();
        }

        @Override
        public Set<String> supportedComparators(Class<?> type) {
            return Set.of("EQUALS");
        }

        @Override
        public <T> String save(T entity) {
            return null;
        }

        @Override
        public <T> void update(T entity) {
        }

        @Override
        public <T> void remove(String id, Class<T> type) {
        }

        @Override
        public <T> void remove(T entity) {
        }

        @Override
        public <T> void refresh(T entity) {
        }
    }

    static class InMemoryQueueService implements CoredeuxExportQueueService {

        private final Map<String, ExportJob> jobs = new LinkedHashMap<>();

        @Override
        public synchronized ExportJob enqueue(ExportRequest request) {
            String uid = java.util.UUID.randomUUID().toString();
            ExportJob job = ExportJob.builder()
                    .uid(uid)
                    .status(ExportStatus.NEW)
                    .createdAt(Instant.now())
                    .request(request)
                    .response(ExportResponse.builder()
                            .uid(uid)
                            .status(ExportStatus.NEW)
                            .format(request.getOptions().getFormat())
                            .fileName(request.getOptions().getFileName())
                            .fields(request.getFieldList().stream().map(ExportField::getPath).toList())
                            .build())
                    .build();
            jobs.put(uid, job);
            return job;
        }

        @Override
        public synchronized Optional<ExportJob> findByUid(String uid) {
            return Optional.ofNullable(jobs.get(uid));
        }

        @Override
        public synchronized long countInProgress() {
            return jobs.values().stream().filter(job -> ExportStatus.IN_PROGRESS.equals(job.getStatus())).count();
        }

        @Override
        public synchronized List<ExportJob> claimNext(int limit) {
            List<ExportJob> claimed = new ArrayList<>();
            for (ExportJob job : jobs.values()) {
                if (ExportStatus.NEW.equals(job.getStatus())) {
                    job.setStatus(ExportStatus.IN_PROGRESS);
                    job.setStartedAt(Instant.now());
                    job.getResponse().setStatus(ExportStatus.IN_PROGRESS);
                    job.getResponse().setStartedAt(job.getStartedAt());
                    claimed.add(job);
                    if (claimed.size() >= limit) {
                        break;
                    }
                }
            }
            return claimed;
        }

        @Override
        public synchronized void updateProgress(String uid, long rowCount) {
            ExportJob job = jobs.get(uid);
            job.getResponse().setRowCount(rowCount);
        }

        @Override
        public synchronized void markCompleted(String uid, ExportResponse response) {
            ExportJob job = jobs.get(uid);
            job.setStatus(ExportStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            response.setUid(uid);
            response.setStatus(ExportStatus.COMPLETED);
            response.setCompletedAt(job.getCompletedAt());
            response.setCreatedAt(job.getCreatedAt());
            response.setStartedAt(job.getStartedAt());
            job.setResponse(response);
        }

        @Override
        public synchronized void markError(String uid, String errorMessage) {
            ExportJob job = jobs.get(uid);
            job.setStatus(ExportStatus.ERROR);
            job.setErrorMessage(errorMessage);
            if (job.getResponse() != null) {
                job.getResponse().setStatus(ExportStatus.ERROR);
                job.getResponse().setErrorMessage(errorMessage);
            }
        }
    }

    static class InMemoryLogService implements CoredeuxExportLogService {

        private final Map<String, List<ExportLogEntry>> logs = new LinkedHashMap<>();

        @Override
        public void info(String uid, String message, Map<String, Object> metadata) {
            append(uid, "INFO", message, null, metadata);
        }

        @Override
        public void warn(String uid, String message, Map<String, Object> metadata) {
            append(uid, "WARN", message, null, metadata);
        }

        @Override
        public void error(String uid, String message, Throwable error, Map<String, Object> metadata) {
            append(uid, "ERROR", message, error, metadata);
        }

        @Override
        public List<ExportLogEntry> findByUid(String uid) {
            return logs.getOrDefault(uid, List.of());
        }

        private void append(String uid, String level, String message, Throwable error, Map<String, Object> metadata) {
            logs.computeIfAbsent(uid, key -> new ArrayList<>()).add(ExportLogEntry.builder()
                    .uid(uid)
                    .timestamp(Instant.now())
                    .level(level)
                    .message(message)
                    .exceptionType(error == null ? null : error.getClass().getName())
                    .metadata(metadata == null ? Map.of() : metadata)
                    .build());
        }
    }

    static class RecordingStorageService extends DefaultCoredeuxFileSystemExportStorageService {

        private final String name;

        RecordingStorageService(String name) {
            super(Path.of(System.getProperty("java.io.tmpdir"), "coredeux-export-test-custom").toString());
            this.name = name;
        }

        @Override
        public ExportStorageArtifact store(com.coredeux.export.model.ExportStorageRequest request) {
            try {
                Path base = Path.of(System.getProperty("java.io.tmpdir"), "coredeux-export-test-custom");
                Files.createDirectories(base);
                Path destination = base.resolve(request.getFileName());
                Files.copy(request.getSourceFile(), destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return ExportStorageArtifact.builder()
                        .storageType("custom-storage")
                        .fileName(request.getFileName())
                        .absolutePath(destination.toAbsolutePath().toString())
                        .relativePath(base.relativize(destination).toString())
                        .url(destination.toUri().toString())
                        .canonicalUrl(destination.toUri().toString())
                        .size(Files.size(destination))
                        .contentType(request.getContentType())
                        .metadata(Map.of("service", name))
                        .build();
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }

    public static class Customer {
        private String name;
        private String email;
        private boolean active;
        private BigDecimal creditLimit;
        private CustomerProfile profile;
        private final List<String> phoneNumbers = new ArrayList<>();
        private final List<Role> roles = new ArrayList<>();
        private final Map<String, String> preferences = new LinkedHashMap<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public BigDecimal getCreditLimit() {
            return creditLimit;
        }

        public void setCreditLimit(BigDecimal creditLimit) {
            this.creditLimit = creditLimit;
        }

        public CustomerProfile getProfile() {
            return profile;
        }

        public void setProfile(CustomerProfile profile) {
            this.profile = profile;
        }

        public List<String> getPhoneNumbers() {
            return phoneNumbers;
        }

        public List<Role> getRoles() {
            return roles;
        }

        public Map<String, String> getPreferences() {
            return preferences;
        }
    }

    public static class CustomerProfile {
        private String biography;
        private java.util.Date legacySignupDate;

        public String getBiography() {
            return biography;
        }

        public void setBiography(String biography) {
            this.biography = biography;
        }

        public java.util.Date getLegacySignupDate() {
            return legacySignupDate;
        }

        public void setLegacySignupDate(java.util.Date legacySignupDate) {
            this.legacySignupDate = legacySignupDate;
        }
    }

    public record Role(String code) {
    }

    static class DateOnlyExportHandler implements com.coredeux.export.handler.CoredeuxExportValueHandler {

        @Override
        public Object handle(com.coredeux.export.handler.ExportValueContext context) {
            assertEquals("profile:legacySignupDate", context.getFieldPath());
            assertTrue(context.getResolvedValue() instanceof java.util.Date);
            return ((java.util.Date) context.getResolvedValue()).toInstant().toString().substring(0, 10);
        }
    }
}
