# Import Into An Existing Spring Boot App

<!-- docs-nav-start -->
[Previous: Native Tour Of The Demo](/miscellaneous/02-native-tour-of-the-demo) | [Documentation Home](/) | [Next: Platform Documentation](/platform/)
<!-- docs-nav-end -->

This tutorial shows how to add Coredeux import to an existing Spring Boot
application that already uses Java 21, JPA, and PostgreSQL.

It is intentionally narrow:

- no new demo app
- no hooks
- no validators
- no export
- no workflow module

The goal is to add import support to a real application that already exists.

If you are a developer, think of this as a drop-in integration guide. If you
are an AI agent reading the repository, this is the smallest useful surface
area for wiring Coredeux import into a Spring Boot host.

## What You Add

You add four things:

1. Coredeux import dependencies
2. Coredeux entity metadata
3. A small import service
4. A controller that accepts JSON and file uploads

The app keeps its existing JPA entities, database, and business code. Coredeux
only takes over the import path.

## 1. Add The Dependencies

For an existing Spring Boot app, add the Coredeux starters that match your host
and persistence stack:

```xml
<dependency>
    <groupId>com.coredeux</groupId>
    <artifactId>coredeux-core-spring-boot-starter</artifactId>
    <version>${coredeux.version}</version>
</dependency>

<dependency>
    <groupId>com.coredeux</groupId>
    <artifactId>coredeux-core-jpa-spring-boot-starter</artifactId>
    <version>${coredeux.version}</version>
</dependency>

<dependency>
    <groupId>com.coredeux</groupId>
    <artifactId>coredeux-import-spring-boot-starter</artifactId>
    <version>${coredeux.version}</version>
</dependency>
```

Keep your normal Spring Boot pieces too:

- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- PostgreSQL driver
- any existing application dependencies

The import starter wires the import pipeline beans. The core starter provides
the runtime contracts. The JPA starter gives Coredeux a storage adapter for
your existing database model.

## 2. Register The Entity You Want To Import

Coredeux import works against entity definitions. For an existing app, that
usually means mapping one of your current JPA entities into Coredeux metadata.
If you only need the global fallback data access service, you can keep using
`coredeux.data-access-service` without defining an entity-specific storage
entry.

Add below properties in `application.properties` / `application.yaml`:

```yaml
coredeux:
  entities:
    config-location: classpath:coredeux-entities.yml
  import:
    default-parser: text
```

If you want entity-specific storage or module overrides, add
`src/main/resources/coredeux-entities.yml`:

```yaml
coredeux:
  customer:
    full-class-name: com.example.customer.domain.Customer
    storage:
      data-access-service: postgresCoredeuxJpaDataAccessService
```

That is the important part for a Postgres-backed app:

- the `full-class-name` points at your existing JPA entity
- the `data-access-service` points at the Coredeux JPA adapter bean

If you want to import another entity later, add another block in the same file.

For a Spring Boot app, keep these settings in `application.yml` or
`application.properties`. If you want, you can also place the same fallback
values in `src/main/resources/META-INF/coredeux.yml`.

## 3. Add A Small Import Service

Keep the controller thin. Put the import flow in a service that knows how to:

- validate JSON import requests
- execute JSON import requests
- parse text files into `ImportRequest`
- parse Excel files into `ImportRequest`
- hand the request to `CoredeuxImportService`

Example:

```java
package com.example.customer.importing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.model.ImportResponse;
import com.coredeux.impex.parser.excel.CoredeuxExcelImportParser;
import com.coredeux.impex.parser.exception.CoredeuxImportParserException;
import com.coredeux.impex.parser.text.CoredeuxTextImportParser;
import com.coredeux.impex.service.CoredeuxImportService;
import com.coredeux.spring.boot.autoconfigure.CoredeuxImportProperties;

@Service
public class CustomerImportService {

    private static final String XLS_CONTENT_TYPE = "application/vnd.ms-excel";
    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final CoredeuxImportService coredeuxImportService;
    private final CoredeuxTextImportParser textImportParser;
    private final CoredeuxExcelImportParser excelImportParser;
    private final CoredeuxImportProperties coredeuxImportProperties;

    public CustomerImportService(CoredeuxImportService coredeuxImportService,
            CoredeuxTextImportParser textImportParser,
            CoredeuxExcelImportParser excelImportParser,
            CoredeuxImportProperties coredeuxImportProperties) {
        this.coredeuxImportService = coredeuxImportService;
        this.textImportParser = textImportParser;
        this.excelImportParser = excelImportParser;
        this.coredeuxImportProperties = coredeuxImportProperties;
    }

    public ImportResponse validateJson(ImportRequest request) {
        return coredeuxImportService.validateData(request);
    }

    public ImportResponse importJson(ImportRequest request) {
        return coredeuxImportService.importData(request);
    }

    public ImportResponse validateFile(MultipartFile file, String sheetName) throws IOException {
        return coredeuxImportService.validateData(parseFile(file, sheetName));
    }

    public ImportResponse importFile(MultipartFile file, String sheetName) throws IOException {
        return coredeuxImportService.importData(parseFile(file, sheetName));
    }

    private ImportRequest parseFile(MultipartFile file, String sheetName) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new CoredeuxImportParserException("Import file must not be empty");
        }

        if (isExcel(file) || "excel".equalsIgnoreCase(defaultParser())) {
            try (InputStream inputStream = file.getInputStream()) {
                return excelImportParser.parse(inputStream, sheetName);
            }
        }

        return textImportParser.parse(new String(file.getBytes(), StandardCharsets.UTF_8));
    }

    private boolean isExcel(MultipartFile file) {
        String contentType = normalize(file.getContentType());
        if (XLS_CONTENT_TYPE.equals(contentType) || XLSX_CONTENT_TYPE.equals(contentType)) {
            return true;
        }
        String filename = normalize(file.getOriginalFilename());
        return filename.endsWith(".xls") || filename.endsWith(".xlsx");
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String defaultParser() {
        String parser = coredeuxImportProperties == null ? null : coredeuxImportProperties.defaultParser();
        return parser == null || parser.isBlank() ? "text" : parser.trim();
    }
}
```

What this service does:

- JSON requests go straight to `CoredeuxImportService`
- text files are parsed with `CoredeuxTextImportParser`
- Excel files are parsed with `CoredeuxExcelImportParser`
- the default parser falls back to `text` if the property is missing

## 4. Add The Controller

Expose four endpoints:

- JSON validate
- JSON import
- file validate
- file import

Example:

```java
package com.example.customer.web;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.model.ImportResponse;
import com.example.customer.importing.CustomerImportService;

@RestController
@RequestMapping("/api/import")
public class CustomerImportController {

    private final CustomerImportService customerImportService;

    public CustomerImportController(CustomerImportService customerImportService) {
        this.customerImportService = customerImportService;
    }

    @PostMapping("/validate")
    public ResponseEntity<ImportResponse> validate(@RequestBody ImportRequest request) {
        return response(customerImportService.validateJson(request));
    }

    @PostMapping
    public ResponseEntity<ImportResponse> importJson(@RequestBody ImportRequest request) {
        return response(customerImportService.importJson(request));
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResponse> importFile(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "sheetName", required = false) String sheetName) throws IOException {
        return response(customerImportService.importFile(file, sheetName));
    }

    @PostMapping(value = "/file/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResponse> validateFile(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "sheetName", required = false) String sheetName) throws IOException {
        return response(customerImportService.validateFile(file, sheetName));
    }

    private ResponseEntity<ImportResponse> response(ImportResponse response) {
        HttpStatus status = response != null && response.hasErrors() ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
}
```

That is enough for a practical import surface in an existing app.

## 5. What The Endpoints Expect

### JSON import

The JSON endpoint accepts an `ImportRequest` body. A minimal example looks like
this:

```json
{
  "options": {
    "passes": 2,
    "failFast": false
  },
  "statements": [
    {
      "operation": "UPSERT",
      "entity": "com.example.customer.domain.Customer",
      "rows": [
        {
          "values": {
            "id": "C-1001",
            "name": "Ada Lovelace",
            "email": "ada@example.com"
          }
        }
      ]
    }
  ]
}
```

Use this when your integration already has a JSON producer, an admin tool, or a
service-to-service import flow.

To call it from the command line:

```bash
curl -X POST http://localhost:8080/api/import \
  -H "Content-Type: application/json" \
  -d @customer-import.json
```

### Text file import

Text files are compiled into the same `ImportRequest` contract by
`CoredeuxTextImportParser`.

Use this when:

- a user uploads a `.import` or other plain text import file
- the file is easier to author by hand
- your operations team wants a human-readable format

The controller above lets you upload the file as multipart form data. If the
upload is not clearly Excel, the service falls back to the text parser.

To call the text upload endpoint:

```bash
curl -X POST http://localhost:8080/api/import/file \
  -F "file=@customers.import"
```

### Excel import

Excel files are compiled into the same `ImportRequest` contract by
`CoredeuxExcelImportParser`.

Use this when:

- the business team prefers spreadsheets
- the import file is maintained in Excel
- you want sheet-based input for larger batches

The `sheetName` parameter is optional. If the file has a clear default sheet,
you can omit it. If the workbook contains multiple sheets, pass the sheet name
explicitly.

To call the Excel upload endpoint:

```bash
curl -X POST http://localhost:8080/api/import/file \
  -F "file=@customers.xlsx" \
  -F "sheetName=Customers"
```

## 6. Spring Boot Properties To Keep In Mind

You usually only need one import-specific property to keep file parsing
predictable:

```yaml
coredeux:
  import:
    default-parser: text
```

You can set it to:

- `text`
- `excel`

If the file metadata is obvious, the service still detects Excel by content type
or extension. The property is the safe fallback when the input is ambiguous.

## 7. What The Existing App Does Not Need To Change

This is the nice part.

You do **not** need to:

- replace your existing JPA repository layer
- add hooks
- add validators
- add export
- add a workflow module
- rewrite the application around Coredeux

You only need to provide:

- the Coredeux entity metadata
- the Coredeux import controller
- the small import service that calls `CoredeuxImportService`

## 8. Suggested Checklist

Use this as the final sanity check:

1. Add `coredeux-core-spring-boot-starter`
2. Add `coredeux-core-jpa-spring-boot-starter`
3. Add `coredeux-import-spring-boot-starter`
4. Add the import properties in `application.properties` / `application.yaml`
5. Add `src/main/resources/coredeux-entities.yml` if you need per-entity overrides
6. Add the import service
7. Add the import controller
8. Test JSON import
9. Test text file import
10. Test Excel import

If those ten steps work, Coredeux import is wired into the existing Spring Boot
app.

<!-- docs-nav-start -->
[Previous: Native Tour Of The Demo](/miscellaneous/02-native-tour-of-the-demo) | [Documentation Home](/) | [Next: Platform Documentation](/platform/)
<!-- docs-nav-end -->
