package com.coredeux.impex.parser.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.model.ImportStatement;
import com.coredeux.impex.parser.exception.CoredeuxImportParserException;

class CoredeuxTextImportParserRobustnessTest {

    private final CoredeuxTextImportParser parser = new CoredeuxTextImportParser();

    @Test
    void shouldParseLongHeaderContinuationAfterStatementOptions() {
        ImportRequest request = parser.parse("""
                MODIFY com.example.Product(query="lower(sku) = :sku and status = :status",query.params="sku,status:state")
                       | sku(handler=lowercaseHandler) | state | name
                       | P-1                           | LIVE  | Demo
                """);

        ImportStatement statement = request.getStatements().get(0);
        assertEquals(3, statement.getColumns().size());
        assertEquals("sku", statement.getColumns().get(0).getName());
        assertEquals("state", statement.getColumns().get(1).getName());
        assertEquals("name", statement.getColumns().get(2).getName());
        assertEquals("P-1", statement.getRows().get(0).getValues().get("sku"));
    }

    @Test
    void shouldHandleEscapedCommasPipesColonsAndTrailingBackslash() {
        ImportRequest request = parser.parse("""
                MODIFY com.example.Product(lookup=code\\:au:sku\\:source:STARTSWITH:false,query.params="sku\\:param:sku\\:column")
                       | sku | code(reference=catalogue\\:version) | tags(default="red\\, blue") | path
                       | P\\|1 | electronics\\:au:Online           | one\\,two,three             | C:\\\\
                """);

        ImportStatement statement = request.getStatements().get(0);
        assertEquals("code:au", statement.getLookup().get(0).getField());
        assertEquals("sku:source", statement.getLookup().get(0).getColumn());
        assertEquals("sku:column", statement.getQuery().getParams().get("sku:param").getColumn());
        assertEquals("catalogue\\:version", statement.getColumns().get(1).getReference());
        assertEquals("red, blue", statement.getColumns().get(2).getDefaultValue());
        assertEquals("P|1", statement.getRows().get(0).getValues().get("sku"));
        assertEquals("electronics\\:au:Online", statement.getRows().get(0).getValues().get("code"));
        assertEquals("one\\,two,three", statement.getRows().get(0).getValues().get("tags"));
        assertEquals("C:\\", statement.getRows().get(0).getValues().get("path"));
    }

    @Test
    void shouldKeepQuotedSeparatorsInsideOptionValues() {
        ImportRequest request = parser.parse("""
                MODIFY com.example.Product(query='json @> {"sku":"P,1"}',query.params="sku,status:state",metadata.note="a,b")
                       | sku(default="A,B") | state | name(default='Pipe | Name')
                       | P-1                | LIVE  | 
                """);

        ImportStatement statement = request.getStatements().get(0);
        assertEquals("json @> {\"sku\":\"P,1\"}", statement.getQuery().getText());
        assertEquals("a,b", statement.getMetadata().get("note"));
        assertEquals("A,B", statement.getColumns().get(0).getDefaultValue());
        assertEquals("Pipe | Name", statement.getColumns().get(2).getDefaultValue());
    }

    @Test
    void shouldParseMetadataTypesDeterministically() {
        ImportRequest request = parser.parse("""
                OPTIONS(passes=2)
                CREATE com.example.Product(metadata.enabled=true,metadata.count=10,metadata.large=2147483648,metadata.code=001)
                       | sku(metadata.active=false,metadata.rank=7,metadata.label=A01)
                       | P-1
                """);

        ImportStatement statement = request.getStatements().get(0);
        assertEquals(Boolean.TRUE, statement.getMetadata().get("enabled"));
        assertEquals(10, statement.getMetadata().get("count"));
        assertEquals(2147483648L, statement.getMetadata().get("large"));
        assertEquals(1, statement.getMetadata().get("code"));
        assertEquals(Boolean.FALSE, statement.getColumns().get(0).getMetadata().get("active"));
        assertEquals(7, statement.getColumns().get(0).getMetadata().get("rank"));
        assertEquals("A01", statement.getColumns().get(0).getMetadata().get("label"));
    }

    @Test
    void shouldTreatBlankCellsAsNullAndPreserveExplicitQuotedBlankAsEmptyString() {
        ImportRequest request = parser.parse("""
                CREATE com.example.Product | sku | empty | blank
                                           | P-1 | ""    | 
                """);

        ImportStatement statement = request.getStatements().get(0);
        assertEquals("", statement.getRows().get(0).getValues().get("empty"));
        assertNull(statement.getRows().get(0).getValues().get("blank"));
    }

    @Test
    void shouldParseQuotedMultilineRowValues() {
        ImportRequest request = parser.parse("""
                CREATE com.example.Article | code | body | title
                                           | A-1  | "Line one
                Line two
                Line three" | Demo
                """);

        ImportStatement statement = request.getStatements().get(0);
        assertEquals("Line one\nLine two\nLine three", statement.getRows().get(0).getValues().get("body"));
        assertEquals("Demo", statement.getRows().get(0).getValues().get("title"));
    }

    @Test
    void shouldKeepSeparatorsAndCommentLookingLinesInsideMultilineValues() {
        ImportRequest request = parser.parse("""
                CREATE com.example.Article | code | body | tags
                                           | A-1  | "first | pipe
                # not a parser comment
                second, comma
                third: colon" | editorial\\,featured
                                           | A-2  | Done | short
                """);

        ImportStatement statement = request.getStatements().get(0);
        assertEquals("first | pipe\n# not a parser comment\nsecond, comma\nthird: colon",
                statement.getRows().get(0).getValues().get("body"));
        assertEquals("editorial\\,featured", statement.getRows().get(0).getValues().get("tags"));
        assertEquals("A-2", statement.getRows().get(1).getValues().get("code"));
    }

    @Test
    void shouldParseMultilineQuotedOptionValues() {
        ImportRequest request = parser.parse("""
                CREATE com.example.Article(metadata.note="line one
                line two") | code | title(default="Hello
                World")
                           | A-1  | 
                """);

        ImportStatement statement = request.getStatements().get(0);
        assertEquals("line one\nline two", statement.getMetadata().get("note"));
        assertEquals("Hello\nWorld", statement.getColumns().get(1).getDefaultValue());
    }

    @Test
    void shouldRejectUnterminatedMultilineQuoteAtStartingLine() {
        CoredeuxImportParserException exception = assertThrows(CoredeuxImportParserException.class,
                () -> parser.parse("""
                        CREATE com.example.Article | code | body
                                                   | A-1  | "Line one
                        Line two
                        """));

        assertTrue(exception.getMessage().contains("Line 2"));
        assertTrue(exception.getMessage().contains("Unterminated quoted value"));
    }

    @ParameterizedTest
    @MethodSource("malformedInputs")
    void shouldRejectMalformedInput(String source, String expectedMessage) {
        CoredeuxImportParserException exception = assertThrows(CoredeuxImportParserException.class,
                () -> parser.parse(source.replace("%n", System.lineSeparator())));

        assertTrue(exception.getMessage().contains(expectedMessage),
                () -> "Expected message to contain '" + expectedMessage + "' but was: " + exception.getMessage());
    }

    static Stream<Arguments> malformedInputs() {
        return Stream.of(
                Arguments.of("OPTIONS(passes=abc)", "Option 'passes' must be an integer"),
                Arguments.of("CREATE com.example.Product | sku(=bad)", "Option key must not be blank"),
                Arguments.of("CREATE com.example.Product | ", "at least one column"),
                Arguments.of("CREATE com.example.Product | sku(", "Invalid header metadata"),
                Arguments.of("CREATE com.example.Product | sku(foo=bar) extra", "Unexpected text after header metadata"),
                Arguments.of("MODIFY com.example.Product(lookup=:sku)", "Lookup field must not be blank"),
                Arguments.of("MODIFY com.example.Product(lookup=a:b:c:d:e)", "Lookup shorthand supports"),
                Arguments.of("MODIFY com.example.Product(query='x',query.params='a:b:c') | a",
                        "Query parameter shorthand supports"),
                Arguments.of("MODIFY com.example.Product(lookup.1.column=sku) | sku", "Lookup field must not be blank"),
                Arguments.of("CREATE com.example.Product | sku | name%n&row(key=bad | P-1 | Demo",
                        "Invalid row key metadata"),
                Arguments.of("CREATE com.example.Product | sku | name%n| P-1 | Demo%n| P-2 | \"unterminated",
                        "Unterminated quoted value"));
    }

    @Test
    void shouldRejectContinuationHeaderWithDuplicateColumns() {
        CoredeuxImportParserException exception = assertThrows(CoredeuxImportParserException.class,
                () -> parser.parse("""
                        MODIFY com.example.Product(query="sku = :sku")
                               | sku | sku
                               | P-1 | P-2
                        """));

        assertTrue(exception.getMessage().contains("Duplicate column name"));
    }
}
