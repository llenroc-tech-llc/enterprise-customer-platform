package com.llenroctech.customerconnect.domain;

public record RefreshedTokens(
        String accessToken,
        String refreshToken
) {
}
