package com.coredeux.demo.drl;

public final class DrlDemoSourceText {

    private DrlDemoSourceText() {
    }

    public static String customerDataAccessSource() {
        return """
                package com.coredeux.demo.drl.dataaccess;

                import java.util.List;
                import java.util.Map;
                import java.util.Set;

                import com.coredeux.core.registry.CoredeuxComponentRegistry;
                import com.coredeux.core.search.SearchParams;
                import com.coredeux.core.search.SearchResult;
                import com.coredeux.core.service.CoredeuxDataAccessService;
                import com.coredeux.demo.domain.Customer;
                import com.coredeux.drl.converter.annotations.DrlDefinition;
                import com.coredeux.drl.converter.annotations.DrlGlobal;
                import com.coredeux.drl.converter.annotations.DrlRule;
                import com.coredeux.drl.model.RuleContext;

                @DrlDefinition("customerDataAccess.drl")
                public class CustomerDrlDataAccessRuleSource {

                    @DrlGlobal
                    public CoredeuxComponentRegistry componentRegistry;

                    @DrlRule(name = "load", when = "$context : RuleContext(method == 'load')")
                    public void load(RuleContext<Customer> $context) {
                        CoredeuxDataAccessService dataAccessService = componentRegistry.getComponent("postgresCoredeuxJpaDataAccessService",
                                CoredeuxDataAccessService.class);
                        String identifier = String.valueOf($context.getParams().get("id"));
                        $context.setOutput((Customer) dataAccessService.load(identifier, Customer.class));
                    }

                    @DrlRule(name = "save", when = "$context : RuleContext(method == 'save')")
                    public void save(RuleContext<String> $context) {
                        CoredeuxDataAccessService dataAccessService = componentRegistry.getComponent("postgresCoredeuxJpaDataAccessService",
                                CoredeuxDataAccessService.class);
                        $context.setOutput(dataAccessService.save($context.getParams().get("entity")));
                    }

                    @DrlRule(name = "update", when = "$context : RuleContext(method == 'update')")
                    public void update(RuleContext<Void> $context) {
                        componentRegistry.getComponent("postgresCoredeuxJpaDataAccessService",
                                CoredeuxDataAccessService.class).update($context.getParams().get("entity"));
                    }

                    @DrlRule(name = "remove", when = "$context : RuleContext(method == 'remove')")
                    public void remove(RuleContext<Void> $context) {
                        componentRegistry.getComponent("postgresCoredeuxJpaDataAccessService",
                                CoredeuxDataAccessService.class).remove($context.getParams().get("entity"));
                    }

                    @DrlRule(name = "loadAll", when = "$context : RuleContext(method == 'loadAll')")
                    public void loadAll(RuleContext<SearchResult<Customer>> $context) {
                        CoredeuxDataAccessService dataAccessService = componentRegistry.getComponent("postgresCoredeuxJpaDataAccessService",
                                CoredeuxDataAccessService.class);
                        List<SearchParams> params = (List<SearchParams>) $context.getParams().getOrDefault("params", List.of());
                        int pageSize = ((Number) $context.getParams().getOrDefault("pageSize", 20)).intValue();
                        int currentPage = ((Number) $context.getParams().getOrDefault("currentPage", 1)).intValue();
                        $context.setOutput(dataAccessService.loadAll(params, Customer.class, pageSize, currentPage));
                    }

                    @DrlRule(name = "supportedComparators", when = "$context : RuleContext(method == 'supportedComparators')")
                    public void supportedComparators(RuleContext<Set<String>> $context) {
                        $context.setOutput(componentRegistry.getComponent("postgresCoredeuxJpaDataAccessService",
                                CoredeuxDataAccessService.class).supportedComparators(Customer.class));
                    }

                    @DrlRule(name = "query", when = "$context : RuleContext(method == 'query')")
                    public void query(RuleContext<SearchResult<Customer>> $context) {
                        CoredeuxDataAccessService dataAccessService = componentRegistry.getComponent("postgresCoredeuxJpaDataAccessService",
                                CoredeuxDataAccessService.class);
                        String query = String.valueOf($context.getParams().get("query"));
                        Map<String, Object> params = (Map<String, Object>) $context.getParams().getOrDefault("params", Map.of());
                        int pageSize = ((Number) $context.getParams().getOrDefault("pageSize", 20)).intValue();
                        int currentPage = ((Number) $context.getParams().getOrDefault("currentPage", 1)).intValue();
                        $context.setOutput(dataAccessService.query(query, params, Customer.class, pageSize, currentPage));
                    }

                    @DrlRule(name = "refresh", when = "$context : RuleContext(method == 'refresh')")
                    public void refresh(RuleContext<Void> $context) {
                        componentRegistry.getComponent("postgresCoredeuxJpaDataAccessService",
                                CoredeuxDataAccessService.class).refresh($context.getParams().get("entity"));
                    }
                }
                """;
    }

    public static String customerValidatorSource() {
        return """
                package com.coredeux.demo.drl.validation;

                import java.util.ArrayList;
                import java.util.List;

                import com.coredeux.core.registry.CoredeuxComponentRegistry;
                import com.coredeux.core.validation.ValidationError;
                import com.coredeux.demo.domain.Customer;
                import com.coredeux.drl.converter.annotations.DrlDefinition;
                import com.coredeux.drl.converter.annotations.DrlGlobal;
                import com.coredeux.drl.converter.annotations.DrlRule;
                import com.coredeux.drl.model.RuleContext;

                @DrlDefinition("customerValidator.drl")
                public class CustomerDrlValidatorRuleSource {

                    @DrlGlobal
                    public CoredeuxComponentRegistry componentRegistry;

                    @DrlRule(name = "validate", when = "$context : RuleContext(method == 'validate')")
                    public void validate(RuleContext<List<ValidationError>> $context) {
                        Customer customer = (Customer) $context.getParams().get("entity");
                        List<ValidationError> errors = new ArrayList<>();
                        if (customer == null) {
                            errors.add(ValidationError.builder().field("entity").message("Customer is required").build());
                        } else {
                            if (customer.getName() == null || customer.getName().isBlank()) {
                                errors.add(ValidationError.builder().field("name").message("Customer name is required").build());
                            }
                            if (customer.getEmail() == null || !customer.getEmail().contains("@")) {
                                errors.add(ValidationError.builder().field("email").message("Customer email must be valid").build());
                            }
                        }
                        $context.setOutput(errors);
                    }
                }
                """;
    }

    public static String customerHookSource() {
        return """
                package com.coredeux.demo.drl.hooks;

                import com.coredeux.core.registry.CoredeuxComponentRegistry;
                import com.coredeux.demo.domain.Customer;
                import com.coredeux.drl.converter.annotations.DrlDefinition;
                import com.coredeux.drl.converter.annotations.DrlGlobal;
                import com.coredeux.drl.converter.annotations.DrlRule;
                import com.coredeux.drl.model.RuleContext;

                @DrlDefinition("customerHook.drl")
                public class CustomerDrlHookRuleSource {

                    @DrlGlobal
                    public CoredeuxComponentRegistry componentRegistry;

                    @DrlRule(name = "beforeSave", when = "$context : RuleContext(method == 'beforeSave')")
                    public void beforeSave(RuleContext<Customer> $context) {
                        Object entity = $context.getParams().get("entity");
                        if (entity instanceof Customer) {
                            ((Customer) entity).setLastLifecycleTouch(java.time.Instant.now());
                        }
                    }

                    @DrlRule(name = "beforeUpdate", when = "$context : RuleContext(method == 'beforeUpdate')")
                    public void beforeUpdate(RuleContext<Customer> $context) {
                        Object entity = $context.getParams().get("entity");
                        if (entity instanceof Customer) {
                            ((Customer) entity).setLastLifecycleTouch(java.time.Instant.now());
                        }
                    }
                }
                """;
    }

    public static String customerAuditSource() {
        return """
                package com.coredeux.demo.drl.audit;

                import com.coredeux.core.registry.CoredeuxComponentRegistry;
                import com.coredeux.demo.domain.Customer;
                import com.coredeux.drl.converter.annotations.DrlDefinition;
                import com.coredeux.drl.converter.annotations.DrlGlobal;
                import com.coredeux.drl.converter.annotations.DrlRule;
                import com.coredeux.drl.model.RuleContext;

                @DrlDefinition("customerAudit.drl")
                public class CustomerDrlAuditRuleSource {

                    @DrlGlobal
                    public CoredeuxComponentRegistry componentRegistry;

                    @DrlRule(name = "audit", when = "$context : RuleContext(method == 'audit')")
                    public void audit(RuleContext<Customer> $context) {
                        $context.setMessage("Audited " + $context.getParams().get("phase"));
                    }
                }
                """;
    }
}
