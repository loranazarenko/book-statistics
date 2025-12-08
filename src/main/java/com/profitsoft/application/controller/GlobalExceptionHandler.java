package com.profitsoft.application.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.profitsoft.application.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException e,
            HttpServletRequest request) {
        log.warn("Resource not found: {}", e.getMessage());

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                e.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        ErrorResponse error = new ErrorResponse();
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setError("VALIDATION_ERROR");
        error.setMessage("Validation failed: " + message);
        error.setPath(request.getRequestURI());
        error.setTimestamp(LocalDateTime.now());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(JsonParseException.class)
    protected ResponseEntity<ErrorResponse> handleJsonParseException(
            JsonParseException e,
            HttpServletRequest request) {
        log.warn("JSON Parse Error at line {}, column {}: {}",
                e.getLocation().getLineNr(),
                e.getLocation().getColumnNr(),
                e.getOriginalMessage());

        String message = String.format(
                "Invalid JSON syntax at line %d, column %d: %s",
                e.getLocation().getLineNr(),
                e.getLocation().getColumnNr(),
                e.getOriginalMessage()
        );

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "JSON_PARSE_ERROR",
                message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(JsonMappingException.class)
    protected ResponseEntity<ErrorResponse> handleJsonMappingException(
            JsonMappingException e,
            HttpServletRequest request) {
        log.warn("JSON Mapping Error at path '{}': {}",
                e.getPathReference(),
                e.getOriginalMessage());

        String message = String.format(
                "Invalid data type at '%s': %s",
                e.getPathReference(),
                e.getOriginalMessage()
        );

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "JSON_MAPPING_ERROR",
                message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(JsonProcessingException.class)
    protected ResponseEntity<ErrorResponse> handleJsonProcessingException(
            JsonProcessingException e,
            HttpServletRequest request) {
        log.warn("JSON Processing Error: {}", e.getMessage());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "JSON_ERROR",
                "Error processing JSON: " + e.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.builder()
                .status(400)
                .error("INVALID_REQUEST")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(400).body(response);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleGenericException(
            Exception e,
            HttpServletRequest request) {
        log.error("Unexpected exception", e);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Something went wrong: " + e.getMessage(),
                request.getRequestURI()
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String errorCode,
            String message,
            String path) {
        ErrorResponse response = new ErrorResponse(
                status.value(),
                errorCode,
                message,
                path,
                LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(response);
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private String path;
        private LocalDateTime timestamp;

        public ErrorResponse(int value, String errorCode,
                             String message, String path,
                             LocalDateTime now) {
            this.status = value;
            this.error = errorCode;
            this.message = message;
            this.path = path;
            this.timestamp = now;
        }
    }
}