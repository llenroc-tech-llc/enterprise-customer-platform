package com.llenroctech.customerconnect.security.handler;

import com.llenroctech.customerconnect.domain.HttpResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.time.LocalDateTime.now;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException
    ) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        HttpResponse httpResponse = HttpResponse.builder()
                .timestamp(now().toString())
                .reason(UNAUTHORIZED.getReasonPhrase())
                .message("Authentication is required.")
                .status(UNAUTHORIZED)
                .statusCode(UNAUTHORIZED.value())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setStatus(UNAUTHORIZED.value());

        objectMapper.writeValue(
                response.getOutputStream(),
                httpResponse
        );
    }
}
