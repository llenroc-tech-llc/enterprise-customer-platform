package com.llenroctech.customerconnect.exception;

import com.llenroctech.customerconnect.request.LoginRequest;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ExceptionTestController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void missingEmailReturnsStructuredValidationError() throws Exception {
        mockMvc.perform(post("/test/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"Password123!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.message").value(
                        "Request validation failed."
                ))
                .andExpect(jsonPath("$.path").value("/test/login"))
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.validationErrors.email[0]")
                        .value("Email is required"));
    }

    @Test
    void missingPasswordReturnsStructuredValidationError() throws Exception {
        mockMvc.perform(post("/test/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password[0]")
                        .value("Password is required"));
    }

    @Test
    void multipleInvalidFieldsReturnAllValidationErrors() throws Exception {
        mockMvc.perform(post("/test/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").isArray())
                .andExpect(jsonPath("$.validationErrors.password").isArray())
                .andExpect(jsonPath("$.validationErrors.email", hasSize(1)))
                .andExpect(jsonPath("$.validationErrors.password", hasSize(1)));
    }

    @Test
    void malformedJsonReturnsSafeBadRequest() throws Exception {
        mockMvc.perform(post("/test/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The request is invalid."))
                .andExpect(jsonPath("$.developerMessage").doesNotExist());
    }

    @Test
    void duplicateEmailReturnsConflictWithoutInternalMessage() throws Exception {
        mockMvc.perform(post("/test/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "An account with that email already exists."
                ))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("users_email_idx")
                )));
    }

    @Test
    void unknownLoginIdentityReturnsUnauthorizedWithoutDisclosure()
            throws Exception {
        mockMvc.perform(post("/test/unknown-login"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(
                        "Authentication failed."
                ))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(
                                "internal account identifier"
                        )
                )));
    }

    @Test
    void databaseConflictDoesNotExposeSql() throws Exception {
        mockMvc.perform(post("/test/data-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "The request conflicts with existing data."
                ))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("INSERT INTO users")
                )));
    }

    @Test
    void domainResourceNotFoundReturnsNotFound() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "The requested resource was not found."
                ));
    }

    @Test
    void unexpectedServiceFailureReturnsGenericServerError() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(
                        "An unexpected error occurred. Please try again."
                ))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("internal service detail")
                )));
    }

    @Test
    void unsupportedMethodReturnsMethodNotAllowed() throws Exception {
        mockMvc.perform(put("/test/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.statusCode").value(405));
    }

    @Test
    void unsupportedMediaTypeReturnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/test/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not-json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.statusCode").value(415));
    }

    @Test
    void nonexistentGetReturnsStructuredNotFound() throws Exception {
        mockMvc.perform(get("/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.path").value("/does-not-exist"))
                .andExpect(jsonPath("$.method").value("GET"));
    }

    @Test
    void nonexistentPostReturnsStructuredNotFound() throws Exception {
        mockMvc.perform(post("/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.path").value("/does-not-exist"))
                .andExpect(jsonPath("$.method").value("POST"));
    }

    @RestController
    @RequestMapping("/test")
    static class ExceptionTestController {

        @PostMapping(
                value = "/login",
                consumes = MediaType.APPLICATION_JSON_VALUE
        )
        void login(@RequestBody @Valid LoginRequest request) {
        }

        @PostMapping("/duplicate")
        void duplicate() {
            throw new UserAlreadyExistsException(
                    "Duplicate key users_email_idx"
            );
        }

        @PostMapping("/data-conflict")
        void dataConflict() {
            throw new DataIntegrityViolationException(
                    "INSERT INTO users; constraint users_email_idx"
            );
        }

        @PostMapping("/unknown-login")
        void unknownLogin() {
            throw new UsernameNotFoundException(
                    "internal account identifier"
            );
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new RoleNotFoundException("Internal role name");
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("internal service detail");
        }
    }
}
