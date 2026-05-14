package com.coredeux.examples.nativejava.postgres;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.examples.nativejava.CoredeuxNativeRuntime;
import com.coredeux.demo.domain.Customer;
import com.coredeux.impex.handler.CoredeuxImportValueHandler;
import com.coredeux.impex.handler.ImportValueHandlerResolver;
import com.coredeux.impex.handler.impl.DefaultCoredeuxImportValueHandler;
import com.coredeux.impex.handler.impl.JsonMapImportHandler;
import com.coredeux.impex.model.ImportLog;
import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.model.ImportResponse;
import com.coredeux.impex.parser.text.CoredeuxTextImportParser;
import com.coredeux.impex.service.CoredeuxImportService;
import com.coredeux.impex.service.impl.DefaultCoredeuxImportService;
import com.coredeux.impex.service.impl.ImportEntityTargetService;

public class PostgresCustomerImportMain {

    private static final String SAMPLE_PATH = "samples/postgres-customers.import";

    public static void main(String[] args) throws IOException {
        try (CoredeuxNativeRuntime runtime = CoredeuxNativeRuntime.create()) {
            // The sample starts as an author-friendly text file, not Java objects.
            String importText = readSampleImportFile();

            // The parser now lives inside coredeux-import, so plain Java callers can compile
            // the same file format into the runtime ImportRequest model without Spring.
            ImportRequest request = new CoredeuxTextImportParser().parse(importText);
            System.out.println("Parsed statements: " + request.getStatements().size());

            // Native applications wire the import service explicitly. A Spring Boot starter can
            // create the same objects as beans later, but nothing here depends on Spring.
            CoredeuxImportService importService = importService(runtime);

            // Structural validation checks the file shape before any database write is attempted.
            ImportResponse validation = importService.validateData(request);
            printLogs("Validation", validation);

            // Import execution goes through CoredeuxService, so the same validators, hooks, audit
            // handlers, data-access routing, and lifecycle phases run as normal create/update code.
            ImportResponse imported = importService.importData(request);
            printLogs("Import", imported);

            // The sample file deliberately includes one blank-name row. The validator rejects that
            // row, while the valid rows are still upserted because failFast is false.
            searchImportedCustomers(runtime);
        }
    }

    private static CoredeuxImportService importService(CoredeuxNativeRuntime runtime) {
        CoredeuxReflectionHelperService reflectionHelperService = new DefaultCoredeuxReflectionHelperService();
        ImportEntityTargetService targetService = new ImportEntityTargetService(reflectionHelperService,
                runtime.entityDefinitionRegistry());
        ImportValueHandlerResolver valueHandlerResolver = new ImportValueHandlerResolver(Map.of(
                ImportValueHandlerResolver.DEFAULT_HANDLER, defaultValueHandler(runtime),
                "jsonMapImportHandler", new JsonMapImportHandler()));
        return new DefaultCoredeuxImportService(runtime.coredeuxService(), reflectionHelperService, targetService,
                valueHandlerResolver);
    }

    private static CoredeuxImportValueHandler defaultValueHandler(CoredeuxNativeRuntime runtime) {
        return new DefaultCoredeuxImportValueHandler(runtime.coredeuxService());
    }

    private static String readSampleImportFile() throws IOException {
        try (InputStream inputStream = PostgresCustomerImportMain.class.getClassLoader()
                .getResourceAsStream(SAMPLE_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing sample import file: " + SAMPLE_PATH);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void printLogs(String label, ImportResponse response) {
        if (response.getLogs().isEmpty()) {
            System.out.println(label + " completed without errors");
            return;
        }
        response.getLogs().forEach(log -> System.out.println(label + " " + format(log)));
    }

    private static String format(ImportLog log) {
        return "[" + log.getSeverity() + "] statement=" + log.getStatementIndex()
                + ", row=" + log.getRowIndex()
                + ", column=" + log.getColumn()
                + ", message=" + log.getMessage();
    }

    private static void searchImportedCustomers(CoredeuxNativeRuntime runtime) {
        SearchResult<Customer> result = runtime.coredeuxService().loadAll(
                List.of(SearchParams.builder()
                        .field("name")
                        .comparator("ANYWHERE")
                        .value("Imported")
                        .build()),
                Customer.class,
                10,
                1);

        System.out.println("Imported customers visible through Coredeux search:");
        result.getResults().forEach(customer -> System.out.println(" - " + customer));
    }
}
