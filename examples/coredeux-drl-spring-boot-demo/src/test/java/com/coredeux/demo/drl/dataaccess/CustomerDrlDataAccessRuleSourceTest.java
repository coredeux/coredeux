package com.coredeux.demo.drl.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxDataAccessService;
import com.coredeux.demo.domain.Customer;
import com.coredeux.drl.model.RuleContext;

import static org.mockito.Mockito.mock;

class CustomerDrlDataAccessRuleSourceTest {

    private CoredeuxDataAccessService dataAccessService;
    private CustomerDrlDataAccessRuleSource source;

    @BeforeEach
    void setUp() {
        dataAccessService = mock(CoredeuxDataAccessService.class);
        CoredeuxComponentRegistry registry = mock(CoredeuxComponentRegistry.class);
        when(registry.getComponent("postgresCoredeuxJpaDataAccessService", CoredeuxDataAccessService.class))
                .thenReturn(dataAccessService);
        source = new CustomerDrlDataAccessRuleSource();
        source.componentRegistry = registry;
    }

    @Test
    void delegatesAllDataAccessOperationsToTheRegistryBackedService() {
        Customer customer = new Customer();
        customer.setName("Alice");
        customer.setEmail("alice@example.com");
        customer.setActive(true);
        customer.setStatus(com.coredeux.demo.domain.CustomerStatus.ACTIVE);
        SearchResult<Customer> searchResult = SearchResult.<Customer>builder().results(List.of(customer)).build();

        when(dataAccessService.load("1", Customer.class)).thenReturn(customer);
        when(dataAccessService.save(customer)).thenReturn("1");
        when(dataAccessService.loadAll(List.of(SearchParams.builder().field("status").comparator("EQUALS").value("ACTIVE").build()),
                Customer.class, 10, 2)).thenReturn(searchResult);
        when(dataAccessService.query("from Customer", Map.of("active", true), Customer.class, 10, 2)).thenReturn(searchResult);
        when(dataAccessService.supportedComparators(Customer.class)).thenReturn(Set.of("EQUALS"));

        RuleContext<Customer> loadContext = RuleContext.<Customer>method("load").param("id", "1");
        source.load(loadContext);
        assertEquals(customer, loadContext.getOutput());

        RuleContext<String> saveContext = RuleContext.<String>method("save").param("entity", customer);
        source.save(saveContext);
        assertEquals("1", saveContext.getOutput());

        RuleContext<Void> updateContext = RuleContext.<Void>method("update").param("entity", customer);
        source.update(updateContext);
        verify(dataAccessService).update(customer);
        assertNull(updateContext.getOutput());

        RuleContext<Void> removeContext = RuleContext.<Void>method("remove").param("entity", customer);
        source.remove(removeContext);
        verify(dataAccessService).remove(customer);

        RuleContext<SearchResult<Customer>> loadAllContext = RuleContext.<SearchResult<Customer>>method("loadAll")
                .param("params", List.of(SearchParams.builder().field("status").comparator("EQUALS").value("ACTIVE").build()))
                .param("pageSize", 10)
                .param("currentPage", 2);
        source.loadAll(loadAllContext);
        assertEquals(searchResult, loadAllContext.getOutput());

        RuleContext<Set<String>> supportedContext = RuleContext.<Set<String>>method("supportedComparators");
        source.supportedComparators(supportedContext);
        assertEquals(Set.of("EQUALS"), supportedContext.getOutput());

        RuleContext<SearchResult<Customer>> queryContext = RuleContext.<SearchResult<Customer>>method("query")
                .param("query", "from Customer")
                .param("params", Map.of("active", true))
                .param("pageSize", 10)
                .param("currentPage", 2);
        source.query(queryContext);
        assertEquals(searchResult, queryContext.getOutput());

        RuleContext<Void> refreshContext = RuleContext.<Void>method("refresh").param("entity", customer);
        source.refresh(refreshContext);
        verify(dataAccessService).refresh(customer);
    }
}
