package com.llenroctech.customerconnect.security;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.llenroctech.customerconnect.security.handler.JwtAuthenticationErrorResponder;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationErrorResponderTest {

    private final JwtAuthenticationErrorResponder responder =
            new JwtAuthenticationErrorResponder(JsonMapper.builder().build());

    @Test
    void expiredTokenProducesSafeUnauthorizedJson() throws Exception {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        responder.writeError(
                request,
                response,
                new TokenExpiredException("raw-secret-token", Instant.now())
        );

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        assertThat(response.getContentAsString())
                .contains("Your session has expired. Please sign in again.")
                .contains("\"path\":\"/user/profile\"")
                .doesNotContain("raw-secret-token");
    }

    @Test
    void disabledAndLockedAccountsProduceForbiddenJson() throws Exception {
        MockHttpServletResponse disabledResponse = new MockHttpServletResponse();
        MockHttpServletResponse lockedResponse = new MockHttpServletResponse();

        responder.writeError(
                request(),
                disabledResponse,
                new DisabledException("internal disabled detail")
        );
        responder.writeError(
                request(),
                lockedResponse,
                new LockedException("internal locked detail")
        );

        assertThat(disabledResponse.getStatus()).isEqualTo(403);
        assertThat(disabledResponse.getContentAsString())
                .contains("Account is disabled.")
                .doesNotContain("internal disabled detail");
        assertThat(lockedResponse.getStatus()).isEqualTo(403);
        assertThat(lockedResponse.getContentAsString())
                .contains("Account is locked.")
                .doesNotContain("internal locked detail");
    }

    @Test
    void unexpectedExceptionProducesGenericInternalServerError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        responder.writeError(
                request(),
                response,
                new IllegalStateException("database and implementation details")
        );

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString())
                .contains("An unexpected authentication error occurred.")
                .doesNotContain("database and implementation details");
    }

    @Test
    void committedResponseIsNotWrittenAgain() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCommitted(true);

        responder.writeError(
                request(),
                response,
                new IllegalStateException("must not be written")
        );

        assertThat(response.getContentAsByteArray()).isEmpty();
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/user/profile"
        );
        request.addHeader("Authorization", "Bearer raw-secret-token");
        return request;
    }
}
