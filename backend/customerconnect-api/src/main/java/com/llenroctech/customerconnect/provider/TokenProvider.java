package com.llenroctech.customerconnect.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.llenroctech.customerconnect.config.JwtProperties;
import com.llenroctech.customerconnect.security.model.CustomerConnectUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class TokenProvider {

    private static final String AUTHORITIES = "authorities";
    private static final String TOKEN_TYPE = "token_type";
    private static final String ACCESS_TOKEN = "access";
    private static final String REFRESH_TOKEN = "refresh";
    private static final String EXPIRED_ATTRIBUTE = "jwt.expired";
    private static final String INVALID_ATTRIBUTE = "jwt.invalid";

    private final JwtProperties properties;
    private final Algorithm algorithm;
    private final JWTVerifier accessTokenVerifier;
    private final JWTVerifier refreshTokenVerifier;

    public TokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.algorithm = Algorithm.HMAC512(properties.secret());
        this.accessTokenVerifier = buildVerifier(ACCESS_TOKEN);
        this.refreshTokenVerifier = buildVerifier(REFRESH_TOKEN);
    }

    public String createAccessToken(CustomerConnectUserPrincipal userPrincipal) {
        Instant issuedAt = Instant.now();

        return JWT.create()
                .withIssuer(properties.issuer())
                .withAudience(properties.audience())
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(
                        issuedAt.plusMillis(properties.accessTokenExpirationMs())
                ))
                .withSubject(userPrincipal.getUsername())
                .withArrayClaim(AUTHORITIES, getAuthoritiesFromUser(userPrincipal))
                .withClaim(TOKEN_TYPE, ACCESS_TOKEN)
                .withJWTId(UUID.randomUUID().toString())
                .sign(algorithm);
    }

    public String createRefreshToken(CustomerConnectUserPrincipal userPrincipal) {
        Instant issuedAt = Instant.now();

        return JWT.create()
                .withIssuer(properties.issuer())
                .withAudience(properties.audience())
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(
                        issuedAt.plusMillis(properties.refreshTokenExpirationMs())
                ))
                .withSubject(userPrincipal.getUsername())
                .withClaim(TOKEN_TYPE, REFRESH_TOKEN)
                .withJWTId(UUID.randomUUID().toString())
                .sign(algorithm);
    }

    public DecodedJWT verifyAccessToken(String token) {
        return accessTokenVerifier.verify(token);
    }

    public DecodedJWT verifyRefreshToken(String token) {
        return refreshTokenVerifier.verify(token);
    }

    public List<GrantedAuthority> getAuthorities(String token) {
        List<String> authorities = verifyAccessToken(token)
                .getClaim(AUTHORITIES)
                .asList(String.class);

        if (authorities == null) {
            return List.of();
        }

        return authorities.stream()
                .map(String::trim)
                .filter(authority -> !authority.isBlank())
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    public Authentication getAuthentication(
            String email,
            List<GrantedAuthority> authorities,
            HttpServletRequest request
    ) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        authorities
                );
        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );
        return authentication;
    }

    public boolean isTokenValid(String email, String token) {
        if (email == null || email.isBlank()) {
            return false;
        }

        try {
            return email.equals(verifyAccessToken(token).getSubject());
        } catch (JWTVerificationException | IllegalArgumentException exception) {
            return false;
        }
    }

    public String getSubject(String token, HttpServletRequest request) {
        try {
            return verifyAccessToken(token).getSubject();
        } catch (TokenExpiredException exception) {
            request.setAttribute(EXPIRED_ATTRIBUTE, exception.getMessage());
            throw exception;
        } catch (InvalidClaimException exception) {
            request.setAttribute(INVALID_ATTRIBUTE, exception.getMessage());
            throw exception;
        } catch (JWTVerificationException exception) {
            request.setAttribute(INVALID_ATTRIBUTE, exception.getMessage());
            throw exception;
        }
    }

    private String[] getAuthoritiesFromUser(
            CustomerConnectUserPrincipal userPrincipal
    ) {
        return userPrincipal.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toArray(String[]::new);
    }

    private JWTVerifier buildVerifier(String tokenType) {
        return JWT.require(algorithm)
                .withIssuer(properties.issuer())
                .withAudience(properties.audience())
                .withClaim(TOKEN_TYPE, tokenType)
                .build();
    }
}
