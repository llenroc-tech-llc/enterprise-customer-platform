package com.llenroctech.customerconnect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        String issuer,
        String audience,
        long accessTokenExpirationMs,
        long refreshTokenExpirationMs
) {
}
