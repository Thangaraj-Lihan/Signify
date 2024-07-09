package com.Quotes.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(QuoteNotFoundException.class)
    public ResponseEntity<Object> handleQuoteNotFoundException(QuoteNotFoundException ex, WebRequest request) {
        logger.error("Quote not found exception: {}", ex.getMessage());
        return handleExceptionInternal(ex, "Quote not found", null, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
        logger.error("Internal server error: {}", ex.getMessage());
        return handleExceptionInternal(ex, "An unexpected error occurred", null, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
