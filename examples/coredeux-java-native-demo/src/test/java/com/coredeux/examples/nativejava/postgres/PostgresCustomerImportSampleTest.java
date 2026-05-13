package com.coredeux.examples.nativejava.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.parser.text.CoredeuxTextImportParser;

class PostgresCustomerImportSampleTest {

    @Test
    void parsesNativePostgresCustomerImportSample() throws IOException {
        String source = readSample();

        ImportRequest request = new CoredeuxTextImportParser().parse(source);

        assertNotNull(request.getOptions());
        assertEquals(1, request.getStatements().size());
        assertEquals(3, request.getStatements().get(0).getRows().size());
    }

    private String readSample() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("samples/postgres-customers.import")) {
            assertNotNull(inputStream);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
