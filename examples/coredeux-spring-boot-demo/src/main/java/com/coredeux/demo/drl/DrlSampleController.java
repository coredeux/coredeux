package com.coredeux.demo.drl;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.service.DRLService;

/**
 * Convenience endpoints for executing the seeded sample DRL rule source.
 *
 * <p>
 * These endpoints let developers try the bundled sample rules without knowing
 * the stored rule code or constructing a custom resolver request first.
 * </p>
 */
@RestController
@RequestMapping("/api/drl/sample")
public class DrlSampleController {

    private static final String SAMPLE_RULE_ID = "demoGreetingRuleSource";

    private final DRLService drlService;

    public DrlSampleController(DRLService drlService) {
        this.drlService = drlService;
    }

    @PostMapping("/greet")
    public RuleContext greet(@RequestBody(required = false) RuleContext context) {
        RuleContext actualContext = sampleContext(context, "greet");
        drlService.execute(SAMPLE_RULE_ID, actualContext);
        return actualContext;
    }

    @PostMapping("/count-facts")
    public RuleContext countFacts(@RequestBody(required = false) RuleContext context) {
        RuleContext actualContext = sampleContext(context, "countFacts");
        drlService.execute(SAMPLE_RULE_ID, actualContext);
        return actualContext;
    }

    private RuleContext sampleContext(RuleContext context, String method) {
        RuleContext actual = context == null ? RuleContext.method(method) : context;
        if (actual.getMethod() == null || actual.getMethod().isBlank()) {
            actual.setMethod(method);
        }
        return actual;
    }
}
