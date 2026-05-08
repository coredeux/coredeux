package com.coredeux.core.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

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
        ValidatorsModuleHandler handler = new ValidatorsModuleHandler(new StaticApplicationContext());
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("validators").enabled(true)
                .handlers(List.of("ignoredValidator")).build();

        handler.execute(new Object(), definition, module, CoredeuxHookPhases.LOAD, OperationContext.empty());
    }

    @Test
    void shouldAggregateValidationErrors() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.getBeanFactory().registerSingleton("validatorOne",
                new RecordingValidator(List.of(ValidationError.builder().field("name").message("required").build())));
        applicationContext.getBeanFactory().registerSingleton("validatorTwo",
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
        ValidatorsModuleHandler handler = new ValidatorsModuleHandler(new StaticApplicationContext());
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
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.getBeanFactory().registerSingleton("typedValidator", new TypedValidator());
        ValidatorsModuleHandler handler = new ValidatorsModuleHandler(applicationContext);
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("validators").enabled(true)
                .handlers(List.of("typedValidator")).build();

        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> handler.execute(new OtherEntity(), definition, module, CoredeuxHookPhases.BEFORE_SAVE,
                        OperationContext.empty()));

        assertTrue(exception.getMessage().contains("does not support entity type"));
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

    private static final class SampleEntity {
    }

    private static final class OtherEntity {
    }
}
