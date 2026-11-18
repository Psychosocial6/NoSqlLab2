package com.example.nosqllab2.exceptions;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> invalidBody(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return respond(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> invalidParameter(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        return respond(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ErrorResponse> applicationError(ApplicationException exception) {
        return respond(exception.status(), exception.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> authenticationFailed() {
        return respond(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> accessDenied() {
        return respond(HttpStatus.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler({ConcurrencyFailureException.class, DataIntegrityViolationException.class})
    ResponseEntity<ErrorResponse> dataConflict() {
        return respond(HttpStatus.CONFLICT, "Data was changed concurrently or conflicts with existing data");
    }

    @ExceptionHandler({DataAccessException.class, AuthenticationServiceException.class})
    ResponseEntity<ErrorResponse> storageFailure(Exception exception) {
        log.error("Storage request failed", exception);
        return respond(HttpStatus.SERVICE_UNAVAILABLE, "Storage is temporarily unavailable");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpectedError(Exception exception) {
        if (exception instanceof org.springframework.web.ErrorResponse error) {
            HttpStatus status = HttpStatus.resolve(error.getStatusCode().value());
            if (status != null && status.is4xxClientError()) {
                return respond(status, status.getReasonPhrase());
            }
        }
        log.error("Unexpected request failure", exception);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private static ResponseEntity<ErrorResponse> respond(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), message));
    }
}
