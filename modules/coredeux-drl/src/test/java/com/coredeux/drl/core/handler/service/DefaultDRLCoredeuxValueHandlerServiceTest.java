package com.coredeux.drl.core.handler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.coredeux.core.exceptions.CoredeuxValueHandlerException;
import com.coredeux.core.handler.CoredeuxValueHandler;
import com.coredeux.core.handler.ValueContext;
import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.registry.InMemoryCoredeuxComponentRegistry;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.service.DRLService;

class DefaultDRLCoredeuxValueHandlerServiceTest {

    @Test
    void delegatesJavaHandlersToTheCoreValueHandlerPipeline() {
        CoredeuxComponentRegistry registry = InMemoryCoredeuxComponentRegistry.builder()
                .component("upperCaseHandler", (CoredeuxValueHandler<String, DemoValueContext>) context -> context.text.toUpperCase())
                .build();
        DefaultDRLCoredeuxValueHandlerService service = new DefaultDRLCoredeuxValueHandlerService(registry,
                mock(DRLService.class));

        String output = service.invoke(" upperCaseHandler ", new DemoValueContext("demo", 7));

        assertEquals("DEMO", output);
    }

    @Test
    void routesDrlHandlersThroughTheDrlRuntimeAndCopiesContextFields() {
        CoredeuxComponentRegistry registry = InMemoryCoredeuxComponentRegistry.builder().build();
        DRLService drlService = mock(DRLService.class);
        DemoValueContext context = new DemoValueContext("demo", 7);

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            RuleContext<String> ruleContext = invocation.getArgument(1, RuleContext.class);
            assertEquals("handle", ruleContext.getMethod());
            assertSame(context, ruleContext.getParams().get("context"));
            assertEquals("demo", ruleContext.getParams().get("text"));
            assertEquals(7, ruleContext.getParams().get("count"));
            assertSame(context, ruleContext.getFacts().get(0));
            ruleContext.setOutput("drl-ok");
            return null;
        }).when(drlService).execute(eq("sample-handler.drl"), any(RuleContext.class));

        DefaultDRLCoredeuxValueHandlerService service = new DefaultDRLCoredeuxValueHandlerService(registry, drlService);

        String output = service.invoke(" sample-handler.drl ", context);

        assertEquals("drl-ok", output);
        verify(drlService, times(1)).execute(eq("sample-handler.drl"), any(RuleContext.class));
    }

    @Test
    void rejectsBlankHandlerNames() {
        DefaultDRLCoredeuxValueHandlerService service = new DefaultDRLCoredeuxValueHandlerService(
                InMemoryCoredeuxComponentRegistry.builder().build(), mock(DRLService.class));

        CoredeuxValueHandlerException exception = assertThrows(CoredeuxValueHandlerException.class,
                () -> service.invoke(" ", new DemoValueContext("demo", 7)));

        assertEquals("Value handler name is required", exception.getMessage());
    }

    private static final class DemoValueContext implements ValueContext {

        private final String text;
        private final int count;

        private DemoValueContext(String text, int count) {
            this.text = text;
            this.count = count;
        }
    }
}
