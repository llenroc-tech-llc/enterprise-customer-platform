package com.llenroctech.customerconnect.domain;

import java.time.LocalDateTime;

public record PasswordResetVerification(
        Long userId,
        LocalDateTime expirationDate
) {
}
