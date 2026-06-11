package com.coredeux.examples.nativejava.drl;

public final class DemoRuleSourceText {

    private DemoRuleSourceText() {
    }

    public static String source() {
        return """
                package com.coredeux.examples.nativejava.drl;

                import com.coredeux.core.registry.CoredeuxComponentRegistry;
                import com.coredeux.drl.converter.annotations.DrlDefinition;
                import com.coredeux.drl.converter.annotations.DrlGlobal;
                import com.coredeux.drl.converter.annotations.DrlRule;
                import com.coredeux.drl.model.RuleContext;
                import com.coredeux.examples.nativejava.drl.DemoGreetingService;

                @DrlDefinition("demoGreetingRuleSource")
                public class DemoRuleSource {

                    @DrlGlobal
                    public CoredeuxComponentRegistry componentRegistry;

                    @DrlRule(name = "greet", when = "$context : RuleContext(method == 'greet')")
                    public void greet(RuleContext<String> $context) {
                        DemoGreetingService greetingService = componentRegistry.getComponent("demoGreetingService",
                                DemoGreetingService.class);
                        $context.setOutput(greetingService.message());
                        $context.setMessage("Greeting rule executed successfully.");
                    }

                    @DrlRule(name = "countFacts", when = "$context : RuleContext(method == 'countFacts')")
                    public void countFacts(RuleContext<Integer> $context) {
                        int count = $context.getFacts() == null ? 0 : $context.getFacts().size();
                        $context.setOutput(count);
                        $context.setMessage("Counted " + count + " fact(s) for the sample rule.");
                    }
                }
                """;
    }
}
