package com.llenroctech.customerconnect.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.llenroctech.customerconnect.config.JwtProperties;
import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.security.model.CustomerConnectUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenProviderTest {

    private static final String SECRET =
            "test-only-secret-that-is-long-enough-for-hmac512";
    private static final String OTHER_SECRET =
            "different-test-only-secret-for-invalid-signatures";
    private static final String ISSUER = "test-customerconnect";
    private static final String AUDIENCE = "test-api";
    private static final String EMAIL = "student@example.com";

    private final TokenProvider provider = provider(
            ISSUER,
            AUDIENCE,
            1_800_000L,
            432_000_000L
    );
    private final CustomerConnectUserPrincipal principal = principal();

    @Test
    void createsAccessTokenWithSubjectAuthoritiesAndType() {
        DecodedJWT token = provider.verifyAccessToken(
                provider.createAccessToken(principal)
        );

        assertThat(token.getSubject()).isEqualTo(EMAIL);
        assertThat(token.getClaim("authorities").asList(String.class))
                .containsExactly("customer:read", "customer:update");
        assertThat(token.getClaim("token_type").asString()).isEqualTo("access");
    }

    @Test
    void createsRefreshTokenWithTypeAndWithoutAuthorities() {
        DecodedJWT token = provider.verifyRefreshToken(
                provider.createRefreshToken(principal)
        );

        assertThat(token.getSubject()).isEqualTo(EMAIL);
        assertThat(token.getClaim("token_type").asString()).isEqualTo("refresh");
        assertThat(token.getClaim("authorities").isMissing()).isTrue();
        assertThat(token.getClaim("permissions").isMissing()).isTrue();
    }

    @Test
    void verifiesValidAccessAndRefreshTokens() {
        assertThat(provider.verifyAccessToken(provider.createAccessToken(principal)))
                .isNotNull();
        assertThat(provider.verifyRefreshToken(provider.createRefreshToken(principal)))
                .isNotNull();
    }

    @Test
    void rejectsRefreshTokenAsAccessToken() {
        String refreshToken = provider.createRefreshToken(principal);

        assertThrows(
                JWTVerificationException.class,
                () -> provider.verifyAccessToken(refreshToken)
        );
    }

    @Test
    void rejectsAccessTokenAsRefreshToken() {
        String accessToken = provider.createAccessToken(principal);

        assertThrows(
                JWTVerificationException.class,
                () -> provider.verifyRefreshToken(accessToken)
        );
    }

    @Test
    void rejectsWrongIssuer() {
        String token = accessToken("wrong-issuer", AUDIENCE, SECRET, futureDate());

        assertThrows(
                JWTVerificationException.class,
                () -> provider.verifyAccessToken(token)
        );
    }

    @Test
    void rejectsWrongAudience() {
        String token = accessToken(ISSUER, "wrong-audience", SECRET, futureDate());

        assertThrows(
                JWTVerificationException.class,
                () -> provider.verifyAccessToken(token)
        );
    }

    @Test
    void rejectsExpiredToken() {
        TokenProvider expiredProvider = provider(
                ISSUER,
                AUDIENCE,
                -1_000L,
                432_000_000L
        );
        String token = expiredProvider.createAccessToken(principal);

        assertThrows(
                JWTVerificationException.class,
                () -> expiredProvider.verifyAccessToken(token)
        );
    }

    @Test
    void rejectsInvalidSignature() {
        String token = accessToken(ISSUER, AUDIENCE, OTHER_SECRET, futureDate());

        assertThrows(
                JWTVerificationException.class,
                () -> provider.verifyAccessToken(token)
        );
    }

    @Test
    void extractsGrantedAuthoritiesFromAccessToken() {
        List<GrantedAuthority> authorities = provider.getAuthorities(
                provider.createAccessToken(principal)
        );

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("customer:read", "customer:update");
    }

    @Test
    void validatesTokenOnlyForMatchingEmail() {
        String token = provider.createAccessToken(principal);

        assertThat(provider.isTokenValid(EMAIL, token)).isTrue();
        assertThat(provider.isTokenValid("other@example.com", token)).isFalse();
        assertThat(provider.isTokenValid(" ", token)).isFalse();
    }

    @Test
    void createsAuthenticatedAuthenticationWithRequestDetails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        List<GrantedAuthority> authorities = provider.getAuthorities(
                provider.createAccessToken(principal)
        );

        Authentication authentication = provider.getAuthentication(
                EMAIL,
                authorities,
                request
        );

        assertThat(authentication)
                .isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isEqualTo(EMAIL);
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("customer:read", "customer:update");
        assertThat(authentication.getDetails()).isNotNull();
    }

    @Test
    void principalHandlesNullBlankWhitespaceAndDuplicatePermissions() {
        assertThat(principalWithPermissions(null).getAuthorities()).isEmpty();
        assertThat(principalWithPermissions("").getAuthorities()).isEmpty();
        assertThat(principalWithPermissions("  , , \t").getAuthorities()).isEmpty();
        assertThat(principalWithPermissions(
                "customer:read, customer:read, customer:update"
        ).getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("customer:read", "customer:update");
    }

    private TokenProvider provider(
            String issuer,
            String audience,
            long accessExpiration,
            long refreshExpiration
    ) {
        return new TokenProvider(new JwtProperties(
                SECRET,
                issuer,
                audience,
                accessExpiration,
                refreshExpiration
        ));
    }

    private CustomerConnectUserPrincipal principal() {
        return principalWithPermissions("customer:read, customer:update");
    }

    private CustomerConnectUserPrincipal principalWithPermissions(
            String permissions
    ) {
        User user = User.builder()
                .id(42L)
                .email(EMAIL)
                .password("encoded-password")
                .enabled(true)
                .isNotLocked(true)
                .build();
        return new CustomerConnectUserPrincipal(
                user,
                permissions
        );
    }

    private String accessToken(
            String issuer,
            String audience,
            String secret,
            Date expiration
    ) {
        return JWT.create()
                .withIssuer(issuer)
                .withAudience(audience)
                .withSubject(EMAIL)
                .withExpiresAt(expiration)
                .withArrayClaim("authorities", new String[]{"customer:read"})
                .withClaim("token_type", "access")
                .sign(Algorithm.HMAC512(secret));
    }

    private Date futureDate() {
        return Date.from(Instant.now().plusSeconds(60));
    }
}
