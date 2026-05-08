package com.coredeux.demo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.coredeux.demo.domain.Address;
import com.coredeux.demo.domain.AddressType;
import com.coredeux.demo.domain.Customer;
import com.coredeux.demo.domain.CustomerOrder;
import com.coredeux.demo.domain.CustomerProfile;
import com.coredeux.demo.domain.CustomerStatus;
import com.coredeux.demo.domain.OrderItem;
import com.coredeux.demo.domain.OrderStatus;
import com.coredeux.demo.domain.Product;
import com.coredeux.demo.domain.ProductCategory;
import com.coredeux.demo.domain.Role;
import com.coredeux.demo.domain.RoleType;
import com.coredeux.impex.model.ImportColumn;
import com.coredeux.impex.model.ImportLookup;
import com.coredeux.impex.model.ImportOperation;
import com.coredeux.impex.model.ImportQuery;
import com.coredeux.impex.model.ImportQueryParam;
import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.model.ImportResponse;
import com.coredeux.impex.model.ImportRow;
import com.coredeux.impex.model.ImportStatement;
import com.coredeux.impex.service.CoredeuxImportService;

@SpringBootTest(properties = "coredeux.demo.bootstrap.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class CoredeuxDemoApplicationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("coredeux_demo_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CoredeuxService coredeuxService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CoredeuxImportService coredeuxImportService;

    @Test
    void shouldCreateComplexGraphAndSearchJsonbThroughCoredeuxService() {
        Role buyerRole = Role.builder().code(RoleType.BUYER).description("Buyer role").build();
        String roleId = coredeuxService.save(buyerRole);
        Role persistedRole = coredeuxService.load(roleId, Role.class);

        Product product = Product.builder()
                .sku("SKU-POSTGRES-1")
                .name("Postgres JSONB Product")
                .price(new BigDecimal("89.90"))
                .active(true)
                .category(ProductCategory.SOFTWARE)
                .tags(new LinkedHashSet<>(List.of("jsonb", "postgres")))
                .attributes(new LinkedHashMap<>(Map.of("edition", "pro", "tier", "gold")))
                .metadata(productMetadata())
                .build();

        String productId = coredeuxService.save(product);
        Product persistedProduct = coredeuxService.load(productId, Product.class);
        assertNotNull(persistedProduct.getCreationTime());
        assertNotNull(persistedProduct.getModifiedTime());

        Customer customer = Customer.builder()
                .name("Charlie Postgres")
                .email("charlie.postgres@example.com")
                .active(true)
                .status(CustomerStatus.ACTIVE)
                .registeredAt(Instant.now())
                .birthDate(LocalDate.of(1988, 9, 3))
                .loyaltyPoints(320)
                .creditLimit(new BigDecimal("8000.00"))
                .phoneNumbers(List.of("+61-400-123-456", "+61-2-9000-1111"))
                .preferences(new LinkedHashMap<>(Map.of("language", "en", "currency", "AUD")))
                .roles(new LinkedHashSet<>(List.of(persistedRole)))
                .build();

        CustomerProfile profile = CustomerProfile.builder()
                .customer(customer)
                .biography("PostgreSQL-backed demo customer")
                .marketingOptIn(true)
                .favoriteTags(new LinkedHashSet<>(List.of("postgres", "jsonb")))
                .build();
        customer.setProfile(profile);

        Address shippingAddress = Address.builder()
                .customer(customer)
                .type(AddressType.SHIPPING)
                .line1("200 PostgreSQL Avenue")
                .city("Melbourne")
                .country("Australia")
                .primaryAddress(true)
                .build();
        customer.getAddresses().add(shippingAddress);

        String customerId = coredeuxService.save(customer);
        Customer persistedCustomer = coredeuxService.load(customerId, Customer.class);
        assertNotNull(persistedCustomer.getCreationTime());
        assertNotNull(persistedCustomer.getModifiedTime());
        assertNotNull(persistedCustomer.getLastLifecycleTouch());
        assertEquals(CustomerStatus.ACTIVE, persistedCustomer.getStatus());
        assertEquals(1, persistedCustomer.getAddresses().size());
        assertEquals(1, persistedCustomer.getRoles().size());

        CustomerOrder order = CustomerOrder.builder()
                .customer(persistedCustomer)
                .shippingAddress(persistedCustomer.getAddresses().get(0))
                .status(OrderStatus.CONFIRMED)
                .createdAt(Instant.now())
                .totalAmount(new BigDecimal("179.80"))
                .notes(List.of("Requires JSONB verification"))
                .build();
        OrderItem item = OrderItem.builder()
                .order(order)
                .product(persistedProduct)
                .quantity(2)
                .unitPrice(new BigDecimal("89.90"))
                .build();
        order.getItems().add(item);

        String orderId = coredeuxService.save(order);
        CustomerOrder persistedOrder = coredeuxService.load(orderId, CustomerOrder.class);
        assertNotNull(persistedOrder.getCreationTime());
        assertNotNull(persistedOrder.getModifiedTime());
        assertEquals(1, persistedOrder.getItems().size());
        assertEquals(OrderStatus.CONFIRMED, persistedOrder.getStatus());

        SearchResult<Product> jsonbTextResult = coredeuxService.loadAll(
                List.of(SearchParams.builder()
                        .field("metadata.catalog.family")
                        .comparator("JSONB(TEXT)")
                        .value(Map.of("operator", "EQUALS", "value", "starter"))
                        .build()),
                Product.class,
                10,
                1);
        assertEquals(1, jsonbTextResult.getResults().size());

        SearchResult<Product> jsonbNumericResult = coredeuxService.loadAll(
                List.of(SearchParams.builder()
                        .field("metadata.metrics.weight")
                        .comparator("JSONB(NUMERIC)")
                        .value(Map.of("operator", "GREATERTHAN", "value", 10))
                        .build()),
                Product.class,
                10,
                1);
        assertEquals(1, jsonbNumericResult.getResults().size());

        SearchResult<Customer> customerList = coredeuxService.loadAll(List.of(), Customer.class, 10, 1);
        String serializedCustomerList = assertDoesNotThrow(() -> objectMapper.writeValueAsString(customerList));
        assertTrue(serializedCustomerList.contains("Charlie Postgres"));
        assertTrue(serializedCustomerList.contains("phoneNumbers"));
    }

    @Test
    void shouldRejectInvalidCustomerThroughConfiguredValidator() {
        Customer invalidCustomer = Customer.builder()
                .name("Invalid")
                .email("invalid-email")
                .active(true)
                .build();

        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> coredeuxService.save(invalidCustomer));

        assertTrue(exception.getValidationErrors().stream()
                .anyMatch(error -> "email".equals(error.getField())));
        assertTrue(exception.getValidationErrors().stream()
                .anyMatch(error -> "status".equals(error.getField())));
    }

    @Test
    void shouldImportDemoGraphThroughCoredeuxImportService() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(
                        ImportStatement.builder()
                                .operation(ImportOperation.UPSERT)
                                .entity(Role.class.getName())
                                .columns(List.of(
                                        column("code", true),
                                        column("description")))
                                .rows(List.of(row("&importRoleBuyer", Map.of(
                                        "code", "BUYER",
                                        "description", "Imported buyer role"))))
                                .build(),
                        ImportStatement.builder()
                                .operation(ImportOperation.UPSERT)
                                .entity(Product.class.getName())
                                .columns(List.of(
                                        column("sku", true),
                                        column("name"),
                                        column("price"),
                                        column("active"),
                                        column("category"),
                                        column("tags")))
                                .rows(List.of(row("&importProduct", Map.of(
                                        "sku", "ITEST-SKU-1001",
                                        "name", "Integration Imported Product",
                                        "price", "42.50",
                                        "active", "true",
                                        "category", "SOFTWARE",
                                        "tags", "integration,import"))))
                                .build(),
                        ImportStatement.builder()
                                .operation(ImportOperation.UPSERT)
                                .entity(Customer.class.getName())
                                .columns(List.of(
                                        column("email", true),
                                        column("name"),
                                        column("active"),
                                        column("status"),
                                        column("registeredAt"),
                                        column("birthDate"),
                                        column("loyaltyPoints"),
                                        column("creditLimit"),
                                        column("phoneNumbers"),
                                        ImportColumn.builder().name("roles").reference("code").build()))
                                .rows(List.of(row("&importCustomer", Map.of(
                                        "email", "integration.import@example.com",
                                        "name", "Integration Import Customer",
                                        "active", "true",
                                        "status", "ACTIVE",
                                        "registeredAt", "2026-05-02T04:00:00Z",
                                        "birthDate", "1990-01-15",
                                        "loyaltyPoints", "44",
                                        "creditLimit", "900.00",
                                        "phoneNumbers", "+61-400-555-111,+61-400-555-222",
                                        "roles", "BUYER"))))
                                .build(),
                        ImportStatement.builder()
                                .operation(ImportOperation.UPSERT)
                                .entity(Address.class.getName())
                                .columns(List.of(
                                        ImportColumn.builder().name("customer").reference("*").build(),
                                        column("line1", true),
                                        column("type"),
                                        column("city"),
                                        column("country"),
                                        column("primaryAddress")))
                                .rows(List.of(row("&importAddress", Map.of(
                                        "customer", "&importCustomer",
                                        "line1", "44 Integration Import Street",
                                        "type", "SHIPPING",
                                        "city", "Sydney",
                                        "country", "Australia",
                                        "primaryAddress", "true"))))
                                .build(),
                        ImportStatement.builder()
                                .operation(ImportOperation.UPSERT)
                                .entity(CustomerOrder.class.getName())
                                .columns(List.of(
                                        ImportColumn.builder().name("customer").reference("*").build(),
                                        ImportColumn.builder().name("shippingAddress").reference("*").build(),
                                        column("status"),
                                        column("createdAt", true),
                                        column("totalAmount"),
                                        column("notes")))
                                .rows(List.of(row("&importOrder", Map.of(
                                        "customer", "&importCustomer",
                                        "shippingAddress", "&importAddress",
                                        "status", "DRAFT",
                                        "createdAt", "2026-05-02T05:00:00Z",
                                        "totalAmount", "85.00",
                                        "notes", "import integration,reference check"))))
                                .build(),
                        ImportStatement.builder()
                                .operation(ImportOperation.UPSERT)
                                .entity(OrderItem.class.getName())
                                .columns(List.of(
                                        ImportColumn.builder().name("order").reference("*").unique(true).build(),
                                        ImportColumn.builder().name("product").reference("sku").unique(true).build(),
                                        column("quantity"),
                                        column("unitPrice")))
                                .rows(List.of(row("&importOrderItem", Map.of(
                                        "order", "&importOrder",
                                        "product", "ITEST-SKU-1001",
                                        "quantity", "2",
                                        "unitPrice", "42.50"))))
                                .build(),
                        ImportStatement.builder()
                                .operation(ImportOperation.UPSERT)
                                .entity(Product.class.getName())
                                .columns(List.of(
                                        column("sku", true),
                                        column("name"),
                                        column("price"),
                                        column("active"),
                                        column("category"),
                                        column("tags")))
                                .rows(List.of(row("&lookupProduct", Map.of(
                                        "sku", "ITEST-LOOKUP-1001",
                                        "name", "Lookup Seed Product",
                                        "price", "51.00",
                                        "active", "true",
                                        "category", "SOFTWARE",
                                        "tags", "lookup,seed"))))
                                .build(),
                        ImportStatement.builder()
                                .operation(ImportOperation.MODIFY)
                                .entity(Product.class.getName())
                                .lookup(List.of(ImportLookup.builder()
                                        .field("sku")
                                        .comparator("STARTSWITH")
                                        .build()))
                                .columns(List.of(
                                        column("sku"),
                                        column("name"),
                                        column("price"),
                                        column("active"),
                                        column("category"),
                                        column("tags")))
                                .rows(List.of(row(null, Map.of(
                                        "sku", "ITEST-LOOKUP-1001",
                                        "name", "Lookup Modified Product",
                                        "price", "52.25",
                                        "active", "true",
                                        "category", "SOFTWARE",
                                        "tags", "lookup,modified"))))
                                .build(),
                        ImportStatement.builder()
                                .operation(ImportOperation.UPSERT)
                                .entity(Product.class.getName())
                                .columns(List.of(
                                        column("sku", true),
                                        column("name"),
                                        column("price"),
                                        column("active"),
                                        column("category"),
                                        column("tags")))
                                .rows(List.of(row("&queryProduct", Map.of(
                                        "sku", "ITEST-QUERY-1001",
                                        "name", "Query Seed Product",
                                        "price", "61.00",
                                        "active", "true",
                                        "category", "SOFTWARE",
                                        "tags", "query,seed"))))
                                .build(),
                        ImportStatement.builder()
                                .operation(ImportOperation.MODIFY)
                                .entity(Product.class.getName())
                                .query(ImportQuery.builder()
                                        .text("select p from Product p where p.sku = :sku")
                                        .params(Map.of("sku", ImportQueryParam.builder().build()))
                                        .build())
                                .columns(List.of(
                                        column("sku"),
                                        column("name"),
                                        column("price"),
                                        column("active"),
                                        column("category"),
                                        column("tags")))
                                .rows(List.of(row(null, Map.of(
                                        "sku", "ITEST-QUERY-1001",
                                        "name", "Query Modified Product",
                                        "price", "62.75",
                                        "active", "true",
                                        "category", "SOFTWARE",
                                        "tags", "query,modified"))))
                                .build()))
                .build();

        ImportResponse response = coredeuxImportService.importData(request);

        assertTrue(response.getLogs().isEmpty(), () -> "Unexpected import logs: " + response.getLogs());
        SearchResult<Customer> importedCustomers = coredeuxService.loadAll(
                List.of(SearchParams.builder()
                        .field("email")
                        .comparator("EQUALS")
                        .value("integration.import@example.com")
                        .build()),
                Customer.class,
                10,
                1);
        assertEquals(1, importedCustomers.getResults().size());
        Customer importedCustomer = importedCustomers.getResults().get(0);
        assertEquals("Integration Import Customer", importedCustomer.getName());
        assertEquals(1, importedCustomer.getRoles().size());
        assertEquals(2, importedCustomer.getPhoneNumbers().size());

        Product lookupProduct = loadSingleProductBySku("ITEST-LOOKUP-1001");
        assertEquals("Lookup Modified Product", lookupProduct.getName());
        assertEquals(0, new BigDecimal("52.25").compareTo(lookupProduct.getPrice()));

        Product queryProduct = loadSingleProductBySku("ITEST-QUERY-1001");
        assertEquals("Query Modified Product", queryProduct.getName());
        assertEquals(0, new BigDecimal("62.75").compareTo(queryProduct.getPrice()));
    }

    private Map<String, Object> productMetadata() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("weight", 12.5);
        metrics.put("version", 3);

        Map<String, Object> catalog = new LinkedHashMap<>();
        catalog.put("family", "starter");
        catalog.put("segment", "b2b");

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("catalog", catalog);
        metadata.put("metrics", metrics);
        return metadata;
    }

    private ImportColumn column(String name) {
        return ImportColumn.builder().name(name).build();
    }

    private ImportColumn column(String name, boolean unique) {
        return ImportColumn.builder().name(name).unique(unique).build();
    }

    private ImportRow row(String key, Map<String, Object> values) {
        return ImportRow.builder().key(key).values(new LinkedHashMap<>(values)).build();
    }

    private Product loadSingleProductBySku(String sku) {
        SearchResult<Product> products = coredeuxService.loadAll(
                List.of(SearchParams.builder()
                        .field("sku")
                        .comparator("EQUALS")
                        .value(sku)
                        .build()),
                Product.class,
                10,
                1);
        assertEquals(1, products.getResults().size());
        return products.getResults().get(0);
    }
}
