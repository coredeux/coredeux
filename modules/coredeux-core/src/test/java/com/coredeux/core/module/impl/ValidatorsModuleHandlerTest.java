package com.coredeux.core.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import com.coredeux.core.testsupport.TestComponentRegistry;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.strategy.CoredeuxHookPhases;
import com.coredeux.core.validation.CoredeuxEntityValidator;
import com.coredeux.core.validation.ValidationError;

class ValidatorsModuleHandlerTest {

    @Test
    void shouldSkipNonWritePhases() {
        ValidatorsModuleHandler handler = new ValidatorsModuleHandler(new TestComponentRegistry());
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("validators").enabled(true)
                .handlers(List.of("ignoredValidator")).build();

        assertDoesNotThrow(() -> handler.execute(new Object(), definition, module, CoredeuxHookPhases.LOAD,
                OperationContext.empty()));
    }

    @Test
    void shouldAggregateValidationErrors() {
        TestComponentRegistry applicationContext = new TestComponentRegistry();
        applicationContext.registerSingleton("validatorOne",
                new RecordingValidator(List.of(ValidationError.builder().field("name").message("required").build())));
        applicationContext.registerSingleton("validatorTwo",
                new RecordingValidator(List.of(ValidationError.builder().field("code").message("invalid").build())));

        ValidatorsModuleHandler handler = new ValidatorsModuleHandler(applicationContext);
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("validators").enabled(true)
                .handlers(List.of("validatorOne", "validatorTwo")).build();

        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> handler.execute(new Object(), definition, module, CoredeuxHookPhases.BEFORE_SAVE,
                        OperationContext.empty()));

        assertEquals(2, exception.getValidationErrors().size());
    }

    @Test
    void shouldFailWhenValidatorBeanCannotBeResolved() {
        ValidatorsModuleHandler handler = new ValidatorsModuleHandler(new TestComponentRegistry());
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("validators").enabled(true)
                .handlers(List.of("missingValidator")).build();

        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> handler.execute(new Object(), definition, module, CoredeuxHookPhases.BEFORE_UPDATE,
                        OperationContext.empty()));

        assertTrue(exception.getMessage().contains("missingValidator"));
    }

    @Test
    void shouldFailWhenValidatorDoesNotSupportEntityType() {
        TestComponentRegistry applicationContext = new TestComponentRegistry();
        applicationContext.registerSingleton("typedValidator", new TypedValidator());
        ValidatorsModuleHandler handler = new ValidatorsModuleHandler(applicationContext);
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("validators").enabled(true)
                .handlers(List.of("typedValidator")).build();

        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> handler.execute(new OtherEntity(), definition, module, CoredeuxHookPhases.BEFORE_SAVE,
                        OperationContext.empty()));

        assertTrue(exception.getMessage().contains("does not support entity type"));
    }

    @Test
    void shouldSkipBlankValidatorsAndNullValidationErrors() {
        TestComponentRegistry applicationContext = new TestComponentRegistry();
        applicationContext.registerSingleton("nullValidator", new NullValidator());
        ValidatorsModuleHandler handler = new ValidatorsModuleHandler(applicationContext);
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        List<String> handlers = new ArrayList<>();
        handlers.add(" ");
        handlers.add(null);
        handlers.add("nullValidator");
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("validators").enabled(true)
                .handlers(handlers).build();

        assertDoesNotThrow(() -> handler.execute(new Object(), definition, module, CoredeuxHookPhases.BEFORE_SAVE,
                OperationContext.empty()));
    }

    private static final class RecordingValidator implements CoredeuxEntityValidator<Object> {

        private final List<ValidationError> errors;

        private RecordingValidator(List<ValidationError> errors) {
            this.errors = errors;
        }

        @Override
        public List<ValidationError> validate(Object entity, CoredeuxEntityDefinition definition, OperationContext context) {
            return errors;
        }
    }

    private static final class TypedValidator implements CoredeuxEntityValidator<SampleEntity> {

        @Override
        public List<ValidationError> validate(SampleEntity entity, CoredeuxEntityDefinition definition,
                OperationContext context) {
            return List.of();
        }
    }

    private static final class NullValidator implements CoredeuxEntityValidator<Object> {

        @Override
        public List<ValidationError> validate(Object entity, CoredeuxEntityDefinition definition,
                OperationContext context) {
            return null;
        }
    }

    private static final class SampleEntity {
    }

    private static final class OtherEntity {
    }
}
