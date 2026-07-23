package com.llenroctech.customerconnect.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.dto.UserDTO;
import com.llenroctech.customerconnect.provider.TokenProvider;
import com.llenroctech.customerconnect.resource.UserResource;
import com.llenroctech.customerconnect.security.handler.ApiAccessDeniedHandler;
import com.llenroctech.customerconnect.security.handler.ApiAuthenticationEntryPoint;
import com.llenroctech.customerconnect.security.handler.JwtAuthenticationErrorResponder;
import com.llenroctech.customerconnect.security.model.CustomerConnectUserPrincipal;
import com.llenroctech.customerconnect.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Date;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = UserResource.class,
        properties = {
                "jwt.secret=test-only-login-security-secret-for-hmac512",
                "jwt.issuer=test-customerconnect",
                "jwt.audience=test-api",
                "jwt.access-token-expiration-ms=1800000",
                "jwt.refresh-token-expiration-ms=432000000"
        }
)
@Import({
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        JwtAuthenticationErrorResponder.class,
        ApiAccessDeniedHandler.class,
        JwtConfiguration.class,
        TokenProvider.class
})
class LoginSecurityTest {

    private static final String EMAIL = "cornell@example.com";
    private static final String PASSWORD = "Password123!";
    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String TEST_SECRET =
            "test-only-login-security-secret-for-hmac512";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private TokenProvider tokenProvider;

    @Test
    void anonymousLoginReachesAuthenticationManagerAndSucceedsForEnabledUser()
            throws Exception {
        when(userDetailsService.loadUserByUsername(EMAIL))
                .thenReturn(principal(true));
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD))
                .thenReturn(true);
        when(userService.getUserByEmail(EMAIL)).thenReturn(userDto());

        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        allOf(
                                containsString("refreshToken="),
                                containsString("HttpOnly"),
                                containsString("SameSite=Lax"),
                                containsString("Path=/user/refresh-token"),
                                containsString("Max-Age=432000"),
                                not(containsString("; Secure"))
                        )
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        not(containsString("JSESSIONID"))
                ));

        verify(userDetailsService).loadUserByUsername(EMAIL);
        verify(userService).getUserByEmail(EMAIL);
    }

    @Test
    void disabledUserIsRejectedAfterUserLookup() throws Exception {
        when(userDetailsService.loadUserByUsername(EMAIL))
                .thenReturn(principal(false));

        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(
                        "Authentication is required to access this resource."
                ));

        verify(userDetailsService).loadUserByUsername(EMAIL);
        verify(userService, never()).getUserByEmail(EMAIL);
    }

    @Test
    void refreshTokenReturnsNewAccessTokenAndRotatesCookie() throws Exception {
        CustomerConnectUserPrincipal principal = principal(true);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD))
                .thenReturn(true);
        when(userService.getUserByEmail(EMAIL)).thenReturn(userDto());

        MvcResult loginResult = mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson()))
                .andExpect(status().isOk())
                .andReturn();
        String originalRefreshToken = extractRefreshToken(loginResult);

        MvcResult refreshResult = mockMvc.perform(post("/user/refresh-token")
                        .cookie(new Cookie(
                                REFRESH_TOKEN_COOKIE,
                                originalRefreshToken
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token refreshed"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        allOf(
                                containsString("refreshToken="),
                                containsString("HttpOnly"),
                                containsString("Path=/user/refresh-token")
                        )
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        not(containsString("JSESSIONID"))
                ))
                .andReturn();

        String replacementRefreshToken = extractRefreshToken(refreshResult);
        assertThat(replacementRefreshToken)
                .isNotBlank()
                .isNotEqualTo(originalRefreshToken);
    }

    @Test
    void missingRefreshCookieReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/user/refresh-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidRefreshTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/user/refresh-token")
                        .cookie(new Cookie(REFRESH_TOKEN_COOKIE, "not-a-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredRefreshTokenReturnsUnauthorized() throws Exception {
        String expiredToken = refreshToken(
                Date.from(Instant.now().minusSeconds(1))
        );

        mockMvc.perform(post("/user/refresh-token")
                        .cookie(new Cookie(REFRESH_TOKEN_COOKIE, expiredToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenCannotBeUsedAsRefreshToken() throws Exception {
        String accessToken = tokenProvider.createAccessToken(principal(true));

        mockMvc.perform(post("/user/refresh-token")
                        .cookie(new Cookie(REFRESH_TOKEN_COOKIE, accessToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validMfaCodeReturnsTokensAfterAtomicConsumption() throws Exception {
        CustomerConnectUserPrincipal principal = principal(true);
        when(userService.verifyCode(EMAIL, "48392157"))
                .thenReturn(true);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        when(userService.getUserByEmail(EMAIL)).thenReturn(userDto());

        mockMvc.perform(post("/user/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verificationJson("48392157")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("refreshToken=")
                ));
    }

    @Test
    void failedMfaVerificationReturnsUnauthorizedWithoutTokens() throws Exception {
        when(userService.verifyCode(EMAIL, "00000000"))
                .thenReturn(false);

        mockMvc.perform(post("/user/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verificationJson("00000000")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        verify(userDetailsService, never()).loadUserByUsername(EMAIL);
    }

    private CustomerConnectUserPrincipal principal(boolean enabled) {
        User user = User.builder()
                .id(1L)
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .enabled(enabled)
                .isNotLocked(true)
                .build();
        return new CustomerConnectUserPrincipal(user, "READ:USER");
    }

    private UserDTO userDto() {
        UserDTO user = new UserDTO();
        user.setId(1L);
        user.setEmail(EMAIL);
        user.setEnabled(true);
        return user;
    }

    private String loginJson() {
        return """
                {
                  "email": "cornell@example.com",
                  "password": "Password123!"
                }
                """;
    }

    private String refreshToken(Date expiration) {
        return JWT.create()
                .withIssuer("test-customerconnect")
                .withAudience("test-api")
                .withSubject(EMAIL)
                .withExpiresAt(expiration)
                .withClaim("token_type", "refresh")
                .sign(Algorithm.HMAC512(TEST_SECRET));
    }

    private String verificationJson(String code) {
        return """
                {
                  "email": "cornell@example.com",
                  "code": "%s"
                }
                """.formatted(code);
    }

    private String extractRefreshToken(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();

        String prefix = REFRESH_TOKEN_COOKIE + "=";
        int valueStart = setCookie.indexOf(prefix) + prefix.length();
        int valueEnd = setCookie.indexOf(';', valueStart);
        return setCookie.substring(valueStart, valueEnd);
    }
}
