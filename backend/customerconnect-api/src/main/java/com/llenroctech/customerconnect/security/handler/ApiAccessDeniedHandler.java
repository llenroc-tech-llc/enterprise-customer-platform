package com.llenroctech.customerconnect.security.handler;

import tools.jackson.databind.ObjectMapper;
import com.llenroctech.customerconnect.domain.HttpResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.time.LocalDateTime.now;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Component
@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        HttpResponse httpResponse = HttpResponse.builder()
                .timestamp(now().toString())
                .reason(FORBIDDEN.getReasonPhrase())
                .message("You do not have permission to access this resource.")
                .status(FORBIDDEN)
                .statusCode(FORBIDDEN.value())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setStatus(FORBIDDEN.value());

        objectMapper.writeValue(response.getOutputStream(), httpResponse);
    }
}
