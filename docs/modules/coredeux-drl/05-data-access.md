# Data Access

<!-- docs-nav-start -->
[Previous: Reference](/modules/coredeux-drl/04-reference) | [Documentation Home](/) | [Next: Validators](/modules/coredeux-drl/06-validators)
<!-- docs-nav-end -->

This page shows the Java-first way to author a DRL-backed data access
implementation.

The expected workflow is:

1. write the Java-like source class first
2. implement every method in the contract
3. run it through `coredeux-drl-devtools`
4. publish the generated DRL text to your external store
5. let `coredeux-drl` resolve, compile, cache, and execute it later

That order matters because it matches how developers naturally work in an IDE.
They write Java first, then convert the source to DRL, then execute the DRL at
runtime.

## Java Source First

The runtime contract for DRL-backed data access is
`DRLCoredeuxDataAccessService`. A real source file should implement the full
interface, not only one or two methods, because the runtime selects behavior by
method name.

The example below shows the full Java-like source that a developer would write
before conversion:

```java
package com.example.customer.drl;

import java.util.Set;

import com.coredeux.core.search.SearchResult;
import com.coredeux.drl.converter.annotations.DrlDefinition;
import com.coredeux.drl.converter.annotations.DrlGlobal;
import com.coredeux.drl.converter.annotations.DrlRule;
import com.coredeux.drl.core.service.DRLCoredeuxDataAccessService;
import com.coredeux.drl.model.RuleContext;
import com.example.customer.Customer;
import com.example.customer.CustomerQueryService;
import com.example.customer.CustomerRepository;

@DrlDefinition("customer-data-access")
public class CustomerDataAccessRules implements DRLCoredeuxDataAccessService {

    @DrlGlobal
    public com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;

    private final CustomerRepository repository;
    private final CustomerQueryService queryService;

    public CustomerDataAccessRules(CustomerRepository repository, CustomerQueryService queryService) {
        this.repository = repository;
        this.queryService = queryService;
    }

    @Override
    @DrlRule(name = "load", when = "$context : RuleContext(method == 'load')")
    public <T> void load(RuleContext<T> $context) {
        String id = String.valueOf($context.getParams().get("identifier"));
        Customer customer = repository.findById(id);
        $context.setOutput((T) customer);
        $context.setMessage(customer == null ? "Customer was not found." : "Customer loaded.");
    }

    @Override
    @DrlRule(name = "save", when = "$context : RuleContext(method == 'save')")
    public void save(RuleContext<String> $context) {
        Customer customer = (Customer) $context.getParams().get("entity");
        Customer saved = repository.save(customer);
        $context.setOutput(saved.getId());
        $context.setMessage("Customer saved successfully.");
    }

    @Override
    @DrlRule(name = "update", when = "$context : RuleContext(method == 'update')")
    public <T> void update(RuleContext<T> $context) {
        Customer customer = (Customer) $context.getParams().get("entity");
        repository.update(customer);
        $context.setMessage("Customer updated successfully.");
    }

    @Override
    @DrlRule(name = "remove", when = "$context : RuleContext(method == 'remove')")
    public <T> void remove(RuleContext<T> $context) {
        Customer customer = (Customer) $context.getParams().get("entity");
        repository.delete(customer);
        $context.setMessage("Customer removed successfully.");
    }

    @Override
    @DrlRule(name = "loadAll", when = "$context : RuleContext(method == 'loadAll')")
    public <T> void loadAll(RuleContext<SearchResult<T>> $context) {
        String query = String.valueOf($context.getParams().get("query"));
        Integer pageSize = (Integer) $context.getParams().get("pageSize");
        Integer currentPage = (Integer) $context.getParams().get("currentPage");
        SearchResult<Customer> result = queryService.loadAll(query, pageSize, currentPage);
        $context.setOutput((SearchResult<T>) result);
        $context.setMessage("Customer page loaded.");
    }

    @Override
    @DrlRule(name = "supportedComparators", when = "$context : RuleContext(method == 'supportedComparators')")
    public void supportedComparators(RuleContext<Set<String>> $context) {
        $context.setOutput(Set.of("EQUALS", "LIKE", "STARTS_WITH"));
        $context.setMessage("Comparator set resolved.");
    }

    @Override
    @DrlRule(name = "query", when = "$context : RuleContext(method == 'query')")
    public <T> void query(RuleContext<SearchResult<T>> $context) {
        String query = String.valueOf($context.getParams().get("query"));
        Integer pageSize = (Integer) $context.getParams().get("pageSize");
        Integer currentPage = (Integer) $context.getParams().get("currentPage");
        SearchResult<Customer> result = queryService.query(query, pageSize, currentPage);
        $context.setOutput((SearchResult<T>) result);
        $context.setMessage("Customer query completed.");
    }

    @Override
    @DrlRule(name = "refresh", when = "$context : RuleContext(method == 'refresh')")
    public <T> void refresh(RuleContext<T> $context) {
        Customer customer = (Customer) $context.getParams().get("entity");
        Customer refreshed = repository.refresh(customer);
        $context.setOutput((T) refreshed);
        $context.setMessage("Customer refreshed.");
    }
}
```

The important part is not the repository names. The important part is that the
class is complete, readable in an IDE, and ready for conversion.

That completeness matters because the converted DRL does not keep the Java
helper methods as reusable DRL methods. If several rule branches need the same
behavior, model it as a separate DRL source or push it behind an external
service that the rule resolves through the component registry.

## Generated DRL

After DevTools converts the source, the runtime consumes a DRL file that
contains the same behavior as separate rules.

```drl
package com.example.customer.drl;

import java.util.Set;
import com.coredeux.core.search.SearchResult;
import com.coredeux.drl.model.RuleContext;
import com.example.customer.Customer;
import com.example.customer.CustomerQueryService;
import com.example.customer.CustomerRepository;

global com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;

rule "load"
when
    $context : RuleContext(method == "load")
then
    String id = String.valueOf($context.getParams().get("identifier"));
    CustomerRepository repository = componentRegistry.getComponent("customerRepository", CustomerRepository.class);
    Customer customer = repository.findById(id);
    $context.setOutput(customer);
    $context.setMessage(customer == null ? "Customer was not found." : "Customer loaded.");
end

rule "save"
when
    $context : RuleContext(method == "save")
then
    CustomerRepository repository = componentRegistry.getComponent("customerRepository", CustomerRepository.class);
    Customer customer = (Customer) $context.getParams().get("entity");
    Customer saved = repository.save(customer);
    $context.setOutput(saved.getId());
    $context.setMessage("Customer saved successfully.");
end

rule "update"
when
    $context : RuleContext(method == "update")
then
    CustomerRepository repository = componentRegistry.getComponent("customerRepository", CustomerRepository.class);
    Customer customer = (Customer) $context.getParams().get("entity");
    repository.update(customer);
    $context.setMessage("Customer updated successfully.");
end

rule "remove"
when
    $context : RuleContext(method == "remove")
then
    CustomerRepository repository = componentRegistry.getComponent("customerRepository", CustomerRepository.class);
    Customer customer = (Customer) $context.getParams().get("entity");
    repository.delete(customer);
    $context.setMessage("Customer removed successfully.");
end

rule "loadAll"
when
    $context : RuleContext(method == "loadAll")
then
    CustomerQueryService queryService = componentRegistry.getComponent("customerQueryService", CustomerQueryService.class);
    String query = String.valueOf($context.getParams().get("query"));
    Integer pageSize = (Integer) $context.getParams().get("pageSize");
    Integer currentPage = (Integer) $context.getParams().get("currentPage");
    SearchResult<Customer> result = queryService.loadAll(query, pageSize, currentPage);
    $context.setOutput(result);
    $context.setMessage("Customer page loaded.");
end

rule "supportedComparators"
when
    $context : RuleContext(method == "supportedComparators")
then
    $context.setOutput(Set.of("EQUALS", "LIKE", "STARTS_WITH"));
    $context.setMessage("Comparator set resolved.");
end

rule "query"
when
    $context : RuleContext(method == "query")
then
    CustomerQueryService queryService = componentRegistry.getComponent("customerQueryService", CustomerQueryService.class);
    String query = String.valueOf($context.getParams().get("query"));
    Integer pageSize = (Integer) $context.getParams().get("pageSize");
    Integer currentPage = (Integer) $context.getParams().get("currentPage");
    SearchResult<Customer> result = queryService.query(query, pageSize, currentPage);
    $context.setOutput(result);
    $context.setMessage("Customer query completed.");
end

rule "refresh"
when
    $context : RuleContext(method == "refresh")
then
    CustomerRepository repository = componentRegistry.getComponent("customerRepository", CustomerRepository.class);
    Customer customer = (Customer) $context.getParams().get("entity");
    Customer refreshed = repository.refresh(customer);
    $context.setOutput(refreshed);
    $context.setMessage("Customer refreshed.");
end
```

## Why The Full Interface Matters

When the Java source implements `DRLCoredeuxDataAccessService`, DevTools sees a
complete contract and can turn each method into a separate rule body. That is
better than showing a shortened fragment because the source remains valid Java
and the IDE can still help with imports, types, and method signatures.

It also makes it obvious to a new developer that:

- the method name is the rule selection key
- `RuleContext` carries inputs and outputs by reference
- the generated DRL will still use the same method names
- the runtime resolves the DRL source by rule id, not by raw Java class name

## What The Rule Receives

The data-access context is usually populated with:

- `params.identifier`
- `params.entity`
- `params.query`
- `params.pageSize`
- `params.currentPage`
- `params.type`
- `facts` containing the entity or any extra facts the caller inserted

Keep the shape predictable so the generated DRL stays easy to read.

## Good Data Access Shape

- keep each method small and focused
- use the component registry for repository or service lookup
- write the result back into `RuleContext.output`
- use `RuleContext.message` for a readable summary
- keep type casts explicit so the generated DRL is easy to debug

## Example Call Site

```java
RuleContext<Customer> context = RuleContext.method("load")
        .param("identifier", "CUST-1001")
        .fact(customer);

drlService.execute("customer-data-access.drl", context);

Customer loaded = context.getOutput();
String message = context.getMessage();
```

## Next Step

If you want to see how validation rules follow the same Java-first shape,
continue to the validators page.

<!-- docs-nav-start -->
[Previous: Reference](/modules/coredeux-drl/04-reference) | [Documentation Home](/) | [Next: Validators](/modules/coredeux-drl/06-validators)
<!-- docs-nav-end -->
