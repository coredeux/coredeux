package com.coredeux.examples.nativejava.postgres;

import java.util.List;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.examples.nativejava.CoredeuxNativeRuntime;
import com.coredeux.examples.nativejava.domain.Customer;

public class PostgresCustomerMain {

    public static void main(String[] args) {
        try (CoredeuxNativeRuntime runtime = CoredeuxNativeRuntime.create()) {
            printEntityDefinition(runtime);

            // The first chapter is creation: Coredeux rejects the bad customer, then persists the good one.
            String id = create(runtime);

            // The returned id is the contract between create and load.
            Customer loaded = load(runtime, id);

            // Update follows the same rule: invalid changes fail before the database is touched.
            Customer updated = update(runtime, loaded);

            // Search proves that the updated entity is now discoverable through the configured attributes.
            search(runtime, updated.getName());

            // Remove accepts the loaded entity instance and clears it from PostgreSQL.
            remove(runtime, updated);
        }
    }

    private static void printEntityDefinition(CoredeuxNativeRuntime runtime) {
        CoredeuxEntityDefinition definition = runtime.entityDefinitionRegistry()
                .findByEntityType(Customer.class)
                .orElseThrow(() -> new IllegalStateException("Missing customer entity definition"));
        System.out.println("Loaded entity: " + definition.getName());
        System.out.println("Class: " + definition.getFullClassName());
        System.out.println("Data access service: " + definition.getStorage().getDataAccessService());
        System.out.println("Attributes: " + definition.getAttributes().size());
    }

    private static String create(CoredeuxNativeRuntime runtime) {
        String id = CoredeuxNativeRuntime.customerId("PG");

        // A blank name is not a valid business object in this demo. The customerNameValidator blocks it.
        Customer invalidCustomer = new Customer(id, " ", "missing-name." + id + "@example.test");
        try {
            runtime.coredeuxService().save(invalidCustomer);
        } catch (CoredeuxValidationException exception) {
            System.out.println("Create rejected blank name");
            printValidationErrors(exception);
        }

        // With a real name, the same service path succeeds and returns the primary key.
        Customer customer = new Customer(id, "Mira Chen", email("Mira Chen", id));
        String createdId = runtime.coredeuxService().save(customer);
        System.out.println("Created PostgreSQL customer id: " + createdId);
        return createdId;
    }

    private static Customer load(CoredeuxNativeRuntime runtime, String id) {
        Customer loaded = runtime.coredeuxService().load(id, Customer.class);
        System.out.println("Loaded by returned id: " + loaded);
        return loaded;
    }

    private static Customer update(CoredeuxNativeRuntime runtime, Customer customer) {
        // The loaded entity can be used for update, but the validator still guards the write.
        Customer invalidUpdate = new Customer(customer.getId(), " ", customer.getEmail());
        try {
            runtime.coredeuxService().update(invalidUpdate);
        } catch (CoredeuxValidationException exception) {
            System.out.println("Update rejected blank name");
            printValidationErrors(exception);
        }

        // A valid update changes both searchable data and ordinary persisted data.
        customer.setName("Mira Chen Updated");
        customer.setEmail("updated." + customer.getEmail());
        runtime.coredeuxService().update(customer);

        Customer updated = runtime.coredeuxService().load(customer.getId(), Customer.class);
        System.out.println("Updated loaded customer: " + updated);
        return updated;
    }

    private static void search(CoredeuxNativeRuntime runtime, String name) {
        SearchResult<Customer> result = runtime.coredeuxService().loadAll(
                List.of(SearchParams.builder()
                        .field("name")
                        .comparator("ANYWHERE")
                        .value(name)
                        .build()),
                Customer.class,
                10,
                1);

        System.out.println("Search page: " + result.getPagination());
        result.getResults().forEach(customer -> System.out.println("Search result: " + customer));
    }

    private static void remove(CoredeuxNativeRuntime runtime, Customer customer) {
        runtime.coredeuxService().remove(customer);
        System.out.println("Removed customer. Load after remove: "
                + runtime.coredeuxService().load(customer.getId(), Customer.class));
    }

    private static void printValidationErrors(CoredeuxValidationException exception) {
        exception.getValidationErrors()
                .forEach(error -> System.out.println(error.getField() + ": " + error.getMessage()));
    }

    private static String email(String name, String id) {
        return name.toLowerCase().replace(" ", ".") + "." + id + "@example.test";
    }
}
