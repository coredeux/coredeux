package com.coredeux.demo.drl;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "coredeux.demo.drl.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
public class DrlDemoDataRunner implements CommandLineRunner {

    private final DemoDrlRuleSourceService ruleSourceService;

    public DrlDemoDataRunner(DemoDrlRuleSourceService ruleSourceService) {
        this.ruleSourceService = ruleSourceService;
    }

    @Override
    public void run(String... args) {
        ruleSourceService.seedDemoRules();
    }
}
