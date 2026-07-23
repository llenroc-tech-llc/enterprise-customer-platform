package com.llenroctech.customerconnect.exception;

import com.llenroctech.customerconnect.domain.HttpResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.time.LocalDateTime.now;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<HttpResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, List<String>> errors = new TreeMap<>();

        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(error.getField(), ignored -> new ArrayList<>())
                    .add(safeValidationMessage(error));
        }
        for (ObjectError error : exception.getBindingResult().getGlobalErrors()) {
            errors.computeIfAbsent("_global", ignored -> new ArrayList<>())
                    .add(safeValidationMessage(error));
        }

        log.warn(
                "Request validation failed for {} {}",
                request.getMethod(),
                request.getRequestURI()
        );
        return response(
                BAD_REQUEST,
                "Request validation failed.",
                request,
                errors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<HttpResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        Map<String, List<String>> errors = new TreeMap<>();
        exception.getConstraintViolations().stream()
                .sorted(java.util.Comparator.comparing(
                        violation -> violation.getPropertyPath().toString()
                ))
                .forEach(violation -> errors
                        .computeIfAbsent(
                                violation.getPropertyPath().toString(),
                                ignored -> new ArrayList<>()
                        )
                        .add(safeConstraintMessage(violation)));

        return response(
                BAD_REQUEST,
                "Request validation failed.",
                request,
                errors
        );
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<HttpResponse> handleBadRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Invalid request for {} {} ({})",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName()
        );
        return response(BAD_REQUEST, "The request is invalid.", request);
    }

    @ExceptionHandler({
            NoResourceFoundException.class,
            NoHandlerFoundException.class
    })
    public ResponseEntity<HttpResponse> handleNotFound(
            Exception exception,
            HttpServletRequest request
    ) {
        return response(NOT_FOUND, "The requested resource was not found.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<HttpResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        return response(
                METHOD_NOT_ALLOWED,
                "The HTTP method is not supported for this resource.",
                request
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<HttpResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        return response(
                UNSUPPORTED_MEDIA_TYPE,
                "The request media type is not supported.",
                request
        );
    }

    @ExceptionHandler({
            BadCredentialsException.class,
            UsernameNotFoundException.class
    })
    public ResponseEntity<HttpResponse> handleAuthenticationFailure(
            Exception exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Authentication failed for {} {} ({})",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName()
        );
        return response(UNAUTHORIZED, "Authentication failed.", request);
    }

    @ExceptionHandler({
            DisabledException.class,
            LockedException.class,
            AccountExpiredException.class,
            CredentialsExpiredException.class,
            AccessDeniedException.class
    })
    public ResponseEntity<HttpResponse> handleForbidden(
            Exception exception,
            HttpServletRequest request
    ) {
        return response(FORBIDDEN, "Access is forbidden.", request);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<HttpResponse> handleDuplicateUser(
            UserAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return response(
                CONFLICT,
                "An account with that email already exists.",
                request
        );
    }

    @ExceptionHandler({
            DataIntegrityViolationException.class,
            SQLIntegrityConstraintViolationException.class
    })
    public ResponseEntity<HttpResponse> handleDataConflict(
            Exception exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Data integrity conflict for {} {} ({})",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName()
        );
        return response(
                CONFLICT,
                "The request conflicts with existing data.",
                request
        );
    }

    @ExceptionHandler({
            EntityNotFoundException.class,
            RoleNotFoundException.class
    })
    public ResponseEntity<HttpResponse> handleDomainNotFound(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return response(NOT_FOUND, "The requested resource was not found.", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<HttpResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return response(BAD_REQUEST, "The request is invalid.", request);
    }

    @ExceptionHandler({
            InvalidPasswordResetTokenException.class,
            ExpiredPasswordResetTokenException.class
    })
    public ResponseEntity<HttpResponse> handlePasswordResetTokenFailure(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Password reset token rejected for {} {} ({})",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName()
        );
        return response(
                BAD_REQUEST,
                "This password reset link is invalid or has expired. " +
                        "Please request a new one.",
                request
        );
    }

    @ExceptionHandler(InvalidAccountVerificationException.class)
    public ResponseEntity<HttpResponse> handleAccountVerificationFailure(
            InvalidAccountVerificationException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Account verification rejected for {} {}",
                request.getMethod(),
                request.getRequestURI()
        );
        return response(
                BAD_REQUEST,
                "This account verification link is invalid.",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<HttpResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unexpected error processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );
        return response(
                INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again.",
                request
        );
    }

    private ResponseEntity<HttpResponse> response(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        return response(status, message, request, null);
    }

    private ResponseEntity<HttpResponse> response(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, List<String>> validationErrors
    ) {
        return ResponseEntity.status(status).body(
                HttpResponse.builder()
                        .timestamp(now().toString())
                        .statusCode(status.value())
                        .status(status)
                        .reason(status.getReasonPhrase())
                        .message(message)
                        .path(request.getRequestURI())
                        .method(request.getMethod())
                        .validationErrors(validationErrors)
                        .build()
        );
    }

    private String safeValidationMessage(ObjectError error) {
        String message = error.getDefaultMessage();
        return message == null || message.isBlank()
                ? "Value is invalid"
                : message;
    }

    private String safeConstraintMessage(ConstraintViolation<?> violation) {
        String message = violation.getMessage();
        return message == null || message.isBlank()
                ? "Value is invalid"
                : message;
    }
}
