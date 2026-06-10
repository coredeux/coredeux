package com.coredeux.demo.drl;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.drl.converter.DrlConversionException;

@RestControllerAdvice
public class DrlDemoExceptionHandler {

    @ExceptionHandler(CoredeuxValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationException(CoredeuxValidationException exception) {
        return Map.of("message", exception.getMessage(), "validationErrors", exception.getValidationErrors());
    }

    @ExceptionHandler(DrlConversionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleConversionException(DrlConversionException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(DrlRuleNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(DrlRuleNotFoundException exception) {
        return Map.of("message", exception.getMessage());
    }
}
