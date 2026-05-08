package com.coredeux.demo.bootstrap;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxService;
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

@Component
@ConditionalOnProperty(value = "coredeux.demo.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataRunner implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemoDataRunner.class);

    private final CoredeuxService coredeuxService;

    public DemoDataRunner(CoredeuxService coredeuxService) {
        this.coredeuxService = coredeuxService;
    }

    @Override
    public void run(String... args) {
        Role buyerRole = Role.builder().code(RoleType.BUYER).description("Standard buyer role").build();
        Role reviewerRole = Role.builder().code(RoleType.REVIEWER).description("Can review orders and catalog").build();
        Role adminRole = Role.builder().code(RoleType.ADMIN).description("Administrative access").build();
        String roleId = coredeuxService.save(buyerRole);
        String reviewerRoleId = coredeuxService.save(reviewerRole);
        String adminRoleId = coredeuxService.save(adminRole);

        Product starterProduct = Product.builder()
                .sku("SKU-JSONB-1001")
                .name("Coredeux Advanced Starter Pack")
                .price(new BigDecimal("149.95"))
                .active(true)
                .category(ProductCategory.SOFTWARE)
                .tags(new LinkedHashSet<>(List.of("starter", "featured")))
                .attributes(new LinkedHashMap<>(Map.of("edition", "enterprise", "support", "premium")))
                .metadata(productMetadata())
                .build();
        Product analyticsProduct = Product.builder()
                .sku("SKU-JSONB-2001")
                .name("Coredeux Analytics Suite")
                .price(new BigDecimal("249.50"))
                .active(true)
                .category(ProductCategory.SERVICE)
                .tags(new LinkedHashSet<>(List.of("analytics", "subscription")))
                .attributes(new LinkedHashMap<>(Map.of("edition", "analytics", "support", "business-hours")))
                .metadata(analyticsMetadata())
                .build();
        Product hardwareProduct = Product.builder()
                .sku("SKU-HARDWARE-3001")
                .name("Coredeux Edge Gateway")
                .price(new BigDecimal("899.00"))
                .active(true)
                .category(ProductCategory.HARDWARE)
                .tags(new LinkedHashSet<>(List.of("edge", "iot")))
                .attributes(new LinkedHashMap<>(Map.of("model", "gw-01", "region", "apac")))
                .metadata(hardwareMetadata())
                .build();
        String starterProductId = coredeuxService.save(starterProduct);
        String analyticsProductId = coredeuxService.save(analyticsProduct);
        String hardwareProductId = coredeuxService.save(hardwareProduct);

        Customer customer = Customer.builder()
                .name("Alice Example")
                .email("alice@example.com")
                .active(true)
                .status(CustomerStatus.ACTIVE)
                .registeredAt(Instant.now())
                .birthDate(LocalDate.of(1991, 4, 12))
                .loyaltyPoints(120)
                .creditLimit(new BigDecimal("5000.00"))
                .phoneNumbers(List.of("+61-400-000-000", "+61-2-9999-0000"))
                .preferences(new LinkedHashMap<>(Map.of("language", "en", "timezone", "Australia/Sydney")))
                .roles(new LinkedHashSet<>(List.of(
                        coredeuxService.load(roleId, Role.class),
                        coredeuxService.load(reviewerRoleId, Role.class))))
                .build();

        CustomerProfile profile = CustomerProfile.builder()
                .customer(customer)
                .biography("Sample customer used by the Coredeux demo application")
                .marketingOptIn(true)
                .favoriteTags(new LinkedHashSet<>(List.of("starter", "jsonb")))
                .build();
        customer.setProfile(profile);

        Address shippingAddress = Address.builder()
                .customer(customer)
                .type(AddressType.SHIPPING)
                .line1("100 Demo Street")
                .city("Sydney")
                .country("Australia")
                .primaryAddress(true)
                .build();
        Address officeAddress = Address.builder()
                .customer(customer)
                .type(AddressType.OFFICE)
                .line1("250 Collaboration Lane")
                .city("Sydney")
                .country("Australia")
                .primaryAddress(false)
                .build();
        customer.getAddresses().add(shippingAddress);
        customer.getAddresses().add(officeAddress);

        String customerId = coredeuxService.save(customer);
        Customer persistedCustomer = coredeuxService.load(customerId, Customer.class);
        Product persistedStarterProduct = coredeuxService.load(starterProductId, Product.class);
        Product persistedAnalyticsProduct = coredeuxService.load(analyticsProductId, Product.class);
        Product persistedHardwareProduct = coredeuxService.load(hardwareProductId, Product.class);

        Customer secondaryCustomer = Customer.builder()
                .name("Bob Operations")
                .email("bob.operations@example.com")
                .active(true)
                .status(CustomerStatus.NEW)
                .registeredAt(Instant.now())
                .birthDate(LocalDate.of(1985, 11, 8))
                .loyaltyPoints(45)
                .creditLimit(new BigDecimal("1500.00"))
                .phoneNumbers(List.of("+61-411-222-333"))
                .preferences(new LinkedHashMap<>(Map.of("language", "en", "timezone", "Australia/Melbourne")))
                .roles(new LinkedHashSet<>(List.of(coredeuxService.load(adminRoleId, Role.class))))
                .addresses(new ArrayList<>())
                .orders(new ArrayList<>())
                .build();
        Address secondaryAddress = Address.builder()
                .customer(secondaryCustomer)
                .type(AddressType.HOME)
                .line1("77 Demo Crescent")
                .city("Melbourne")
                .country("Australia")
                .primaryAddress(true)
                .build();
        secondaryCustomer.getAddresses().add(secondaryAddress);
        String secondaryCustomerId = coredeuxService.save(secondaryCustomer);
        Customer persistedSecondaryCustomer = coredeuxService.load(secondaryCustomerId, Customer.class);

        CustomerOrder order = CustomerOrder.builder()
                .customer(persistedCustomer)
                .shippingAddress(persistedCustomer.getAddresses().get(0))
                .status(OrderStatus.CONFIRMED)
                .createdAt(Instant.now())
                .totalAmount(new BigDecimal("299.90"))
                .notes(List.of("Created from demo runner", "Contains JSONB-backed product"))
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(persistedStarterProduct)
                .quantity(2)
                .unitPrice(new BigDecimal("149.95"))
                .build();
        OrderItem secondItem = OrderItem.builder()
                .order(order)
                .product(persistedAnalyticsProduct)
                .quantity(1)
                .unitPrice(new BigDecimal("249.50"))
                .build();
        order.getItems().add(item);
        order.getItems().add(secondItem);
        String orderId = coredeuxService.save(order);

        CustomerOrder secondaryOrder = CustomerOrder.builder()
                .customer(persistedSecondaryCustomer)
                .shippingAddress(persistedSecondaryCustomer.getAddresses().get(0))
                .status(OrderStatus.DRAFT)
                .createdAt(Instant.now())
                .totalAmount(new BigDecimal("899.00"))
                .notes(List.of("Hardware onboarding order"))
                .items(new ArrayList<>())
                .build();
        OrderItem hardwareItem = OrderItem.builder()
                .order(secondaryOrder)
                .product(persistedHardwareProduct)
                .quantity(1)
                .unitPrice(new BigDecimal("899.00"))
                .build();
        secondaryOrder.getItems().add(hardwareItem);
        String secondaryOrderId = coredeuxService.save(secondaryOrder);

        persistedCustomer.setLoyaltyPoints(240);
        // The loaded entity is detached here, so rebuild lazy collections from in-memory state before mutating them.
        Map<String, String> updatedPreferences = new LinkedHashMap<>(customer.getPreferences());
        updatedPreferences.put("favoriteModule", "audit");
        persistedCustomer.setPreferences(updatedPreferences);
        coredeuxService.update(persistedCustomer);

        SearchResult<Product> jsonbResults = coredeuxService.loadAll(
                List.of(SearchParams.builder()
                        .field("metadata.catalog.family")
                        .comparator("JSONB(TEXT)")
                        .value(Map.of("operator", "EQUALS", "value", "starter"))
                        .build()),
                Product.class,
                10,
                1);
        SearchResult<Product> numericJsonbResults = coredeuxService.loadAll(
                List.of(SearchParams.builder()
                        .field("metadata.metrics.weight")
                        .comparator("JSONB(NUMERIC)")
                        .value(Map.of("operator", "GREATERTHAN", "value", 10))
                        .build()),
                Product.class,
                10,
                1);

        LOG.info(
                "Coredeux PostgreSQL demo bootstrap completed. roles={}, products={}, customers={}, orders={} ({} and {}) JSONB text hits={}, JSONB numeric hits={}",
                3, 3, 2, 2, orderId, secondaryOrderId, jsonbResults.getResults().size(),
                numericJsonbResults.getResults().size());
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

    private Map<String, Object> analyticsMetadata() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("weight", 15.2);
        metrics.put("version", 5);

        Map<String, Object> catalog = new LinkedHashMap<>();
        catalog.put("family", "analytics");
        catalog.put("segment", "enterprise");

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("catalog", catalog);
        metadata.put("metrics", metrics);
        return metadata;
    }

    private Map<String, Object> hardwareMetadata() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("weight", 28.0);
        metrics.put("version", 2);

        Map<String, Object> catalog = new LinkedHashMap<>();
        catalog.put("family", "edge");
        catalog.put("segment", "iot");

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("catalog", catalog);
        metadata.put("metrics", metrics);
        return metadata;
    }
}
