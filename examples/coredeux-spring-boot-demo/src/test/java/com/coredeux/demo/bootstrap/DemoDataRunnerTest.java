package com.coredeux.demo.bootstrap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

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

class DemoDataRunnerTest {

    @Test
    void shouldBootstrapDemoDataWithoutThrowing() {
        CoredeuxService coredeuxService = mock(CoredeuxService.class);
        when(coredeuxService.save(any())).thenAnswer(invocation -> invocation.getArgument(0).getClass().getSimpleName() + "-1");
        when(coredeuxService.load(anyString(), eq(Role.class))).thenAnswer(invocation -> role(RoleType.BUYER));
        when(coredeuxService.load(anyString(), eq(Product.class))).thenAnswer(invocation -> product("P", "Sample"));
        when(coredeuxService.load(anyString(), eq(Customer.class))).thenAnswer(invocation -> customer("Sample", "sample@example.com"));
        @SuppressWarnings({ "unchecked", "rawtypes" })
        SearchResult emptyProducts = SearchResult.builder().results(List.of()).build();
        when(coredeuxService.loadAll(any(), eq(Product.class), anyInt(), anyInt())).thenReturn(emptyProducts);

        DemoDataRunner runner = new DemoDataRunner(coredeuxService);
        assertDoesNotThrow(() -> runner.run());

        verify(coredeuxService, times(3)).save(any(Role.class));
        verify(coredeuxService, times(3)).save(any(Product.class));
        verify(coredeuxService, times(2)).save(any(Customer.class));
        verify(coredeuxService, times(2)).save(any(CustomerOrder.class));
        verify(coredeuxService, times(1)).update(any(Customer.class));
        verify(coredeuxService, times(2)).loadAll(any(), eq(Product.class), anyInt(), anyInt());
    }

    private Role role(RoleType type) {
        return Role.builder().code(type).description(type.name()).build();
    }

    private Product product(String sku, String name) {
        return Product.builder()
                .sku(sku)
                .name(name)
                .price(new BigDecimal("10.00"))
                .active(true)
                .category(ProductCategory.SOFTWARE)
                .tags(new LinkedHashSet<>(List.of("tag")))
                .attributes(new LinkedHashMap<>(Map.of()))
                .build();
    }

    private Customer customer(String name, String email) {
        Customer customer = Customer.builder()
                .name(name)
                .email(email)
                .active(true)
                .status(CustomerStatus.ACTIVE)
                .registeredAt(Instant.now())
                .birthDate(LocalDate.of(1990, 1, 1))
                .loyaltyPoints(10)
                .creditLimit(new BigDecimal("100.00"))
                .phoneNumbers(List.of("123"))
                .preferences(new LinkedHashMap<>())
                .roles(new LinkedHashSet<>(List.of(role(RoleType.BUYER))))
                .build();
        CustomerProfile profile = CustomerProfile.builder().customer(customer).biography("bio").build();
        customer.setProfile(profile);
        Address address = Address.builder()
                .customer(customer)
                .type(AddressType.HOME)
                .line1("1 Main Street")
                .city("Sydney")
                .country("Australia")
                .primaryAddress(true)
                .build();
        customer.getAddresses().add(address);
        return customer;
    }
}
