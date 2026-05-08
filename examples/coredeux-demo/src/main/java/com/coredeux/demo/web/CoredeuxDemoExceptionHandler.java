package com.coredeux.demo.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.impex.parser.exception.CoredeuxImportParserException;

@RestControllerAdvice
public class CoredeuxDemoExceptionHandler {

    @ExceptionHandler(CoredeuxValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationException(CoredeuxValidationException exception) {
        return Map.of(
                "message", exception.getMessage(),
                "validationErrors", exception.getValidationErrors());
    }

    @ExceptionHandler(CoredeuxImportParserException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleImportParserException(CoredeuxImportParserException exception) {
        return Map.of("message", exception.getMessage());
    }
}
