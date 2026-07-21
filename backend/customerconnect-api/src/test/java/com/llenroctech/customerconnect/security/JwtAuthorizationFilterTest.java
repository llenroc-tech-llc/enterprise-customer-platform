package com.llenroctech.customerconnect.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.llenroctech.customerconnect.config.JwtConfiguration;
import com.llenroctech.customerconnect.config.SecurityConfig;
import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.dto.UserDTO;
import com.llenroctech.customerconnect.provider.TokenProvider;
import com.llenroctech.customerconnect.resource.UserResource;
import com.llenroctech.customerconnect.security.filter.JwtAuthorizationFilter;
import com.llenroctech.customerconnect.security.handler.ApiAccessDeniedHandler;
import com.llenroctech.customerconnect.security.handler.ApiAuthenticationEntryPoint;
import com.llenroctech.customerconnect.security.model.CustomerConnectUserPrincipal;
import com.llenroctech.customerconnect.service.UserService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = UserResource.class,
        properties = {
                "jwt.secret=test-only-authorization-secret-for-hmac512",
                "jwt.issuer=test-customerconnect",
                "jwt.audience=test-api",
                "jwt.access-token-expiration-ms=1800000",
                "jwt.refresh-token-expiration-ms=432000000"
        }
)
@Import({
        SecurityConfig.class,
        JwtAuthorizationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        JwtConfiguration.class,
        TokenProvider.class
})
class JwtAuthorizationFilterTest {

    private static final String EMAIL = "authorized@example.com";
    private static final String SECRET =
            "test-only-authorization-secret-for-hmac512";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private JwtAuthorizationFilter authorizationFilter;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private BCryptPasswordEncoder passwordEncoder;

    @Test
    void publicEndpointWithoutTokenRemainsAccessible() throws Exception {
        UserDTO created = userDto();
        when(userService.createUser(any(User.class))).thenReturn(created);

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Test",
                                  "lastName": "User",
                                  "email": "authorized@example.com",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/user/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(
                        "Authentication is required to access this resource."
                ));
    }

    @Test
    void validAccessTokenCreatesCurrentPrincipalAndContinuesChain()
            throws Exception {
        CustomerConnectUserPrincipal current = principal(
                true,
                true,
                "READ:USER"
        );
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(current);
        when(userService.getUserByEmail(EMAIL)).thenReturn(userDto());

        mockMvc.perform(get("/user/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value(EMAIL));

        verify(userDetailsService).loadUserByUsername(EMAIL);
        verify(userService).getUserByEmail(EMAIL);
    }

    @Test
    void authoritiesComeFromCurrentUserAndInsufficientAuthorityReturnsForbidden()
            throws Exception {
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(
                principal(true, true, "READ:USER")
        );

        mockMvc.perform(delete("/user/delete/42")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.reason").value(
                        "You do not have permission to access this resource."
                ));
    }

    @Test
    void currentUserAuthorityIsAvailableToSpringSecurity() throws Exception {
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(
                principal(true, true, "DELETE:USER")
        );

        mockMvc.perform(delete("/user/delete/42")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void expiredAccessTokenReturnsUnauthorized() throws Exception {
        assertUnauthorized(accessToken(
                SECRET,
                "access",
                Date.from(Instant.now().minusSeconds(1))
        ));
    }

    @Test
    void malformedTokenReturnsUnauthorized() throws Exception {
        assertUnauthorized("not-a-jwt");
    }

    @Test
    void invalidSignatureReturnsUnauthorized() throws Exception {
        assertUnauthorized(accessToken(
                "different-test-secret-for-invalid-signature-hmac512",
                "access",
                Date.from(Instant.now().plusSeconds(60))
        ));
    }

    @Test
    void refreshTokenAsBearerReturnsUnauthorized() throws Exception {
        assertUnauthorized(tokenProvider.createRefreshToken(
                principal(true, true, "READ:USER")
        ));
    }

    @Test
    void disabledUserWithValidTokenReturnsUnauthorized() throws Exception {
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(
                principal(false, true, "READ:USER")
        );
        assertUnauthorized(accessToken());
    }

    @Test
    void lockedUserWithValidTokenReturnsUnauthorized() throws Exception {
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(
                principal(true, false, "READ:USER")
        );
        assertUnauthorized(accessToken());
    }

    @Test
    void existingAuthenticationIsNotOverwrittenAndChainContinues()
            throws ServletException, IOException {
        UsernamePasswordAuthenticationToken existing =
                UsernamePasswordAuthenticationToken.authenticated(
                        "existing-principal",
                        null,
                        java.util.List.of()
                );
        SecurityContextHolder.getContext().setAuthentication(existing);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, bearer("invalid-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        try {
            authorizationFilter.doFilter(request, response, chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .isSameAs(existing);
            assertThat(chain.getRequest()).isSameAs(request);
            verify(userDetailsService, never()).loadUserByUsername(any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void assertUnauthorized(String token) throws Exception {
        mockMvc.perform(get("/user/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(
                        "Authentication is required to access this resource."
                ));
    }

    private String accessToken() {
        return tokenProvider.createAccessToken(
                principal(true, true, "TOKEN:STALE")
        );
    }

    private String accessToken(String secret, String type, Date expiration) {
        return JWT.create()
                .withIssuer("test-customerconnect")
                .withAudience("test-api")
                .withSubject(EMAIL)
                .withExpiresAt(expiration)
                .withClaim("token_type", type)
                .sign(Algorithm.HMAC512(secret));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private CustomerConnectUserPrincipal principal(
            boolean enabled,
            boolean notLocked,
            String permissions
    ) {
        User user = User.builder()
                .id(42L)
                .email(EMAIL)
                .password("encoded-password")
                .enabled(enabled)
                .isNotLocked(notLocked)
                .build();
        return new CustomerConnectUserPrincipal(user, permissions);
    }

    private UserDTO userDto() {
        UserDTO user = new UserDTO();
        user.setId(42L);
        user.setEmail(EMAIL);
        user.setEnabled(true);
        user.setNotLocked(true);
        return user;
    }
}
