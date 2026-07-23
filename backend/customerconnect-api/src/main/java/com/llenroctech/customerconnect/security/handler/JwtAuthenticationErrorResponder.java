package com.llenroctech.customerconnect.security.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.llenroctech.customerconnect.domain.HttpResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.time.LocalDateTime.now;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationErrorResponder {

    private final ObjectMapper objectMapper;

    public void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            Exception exception
    ) throws IOException {
        if (response.isCommitted()) {
            log.debug(
                    "Authentication error response already committed for path {}",
                    request.getRequestURI()
            );
            return;
        }

        ErrorDetails error = classify(exception);
        String path = request.getRequestURI();

        if (error.status().is5xxServerError()) {
            log.error(
                    "Unexpected authentication error for path {} ({})",
                    path,
                    exception.getClass().getSimpleName(),
                    exception
            );
        } else {
            log.warn(
                    "Authentication rejected for path {} ({})",
                    path,
                    exception.getClass().getSimpleName()
            );
        }

        HttpResponse httpResponse = HttpResponse.builder()
                .timestamp(now().toString())
                .statusCode(error.status().value())
                .status(error.status())
                .reason(error.status().getReasonPhrase())
                .message(error.message())
                .path(path)
                .build();

        response.setStatus(error.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), httpResponse);
    }

    private ErrorDetails classify(Exception exception) {
        if (exception instanceof TokenExpiredException) {
            return new ErrorDetails(UNAUTHORIZED, "Access token has expired.");
        }
        if (exception instanceof DisabledException) {
            return new ErrorDetails(FORBIDDEN, "Account is disabled.");
        }
        if (exception instanceof LockedException) {
            return new ErrorDetails(FORBIDDEN, "Account is locked.");
        }
        if (exception instanceof JWTVerificationException
                || exception instanceof IllegalArgumentException) {
            return new ErrorDetails(UNAUTHORIZED, "Access token is invalid.");
        }
        if (exception instanceof BadCredentialsException
                || exception instanceof UsernameNotFoundException) {
            return new ErrorDetails(UNAUTHORIZED, "Authentication failed.");
        }
        return new ErrorDetails(
                INTERNAL_SERVER_ERROR,
                "An unexpected authentication error occurred."
        );
    }

    private record ErrorDetails(HttpStatus status, String message) {
    }
}
