package com.coredeux.demo.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.coredeux.core.exceptions.CoredeuxValidationException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class CoredeuxDemoExceptionHandler {

    @ExceptionHandler(CoredeuxValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationException(CoredeuxValidationException exception) {
    	log.error(exception.getMessage(), exception);
        return Map.of(
                "message", exception.getMessage(),
                "validationErrors", exception.getValidationErrors());
    }
}
