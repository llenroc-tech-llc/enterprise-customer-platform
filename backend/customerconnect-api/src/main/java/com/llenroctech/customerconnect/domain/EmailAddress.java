package com.llenroctech.customerconnect.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public final class EmailAddress {

    private static final Pattern VALID_EMAIL = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );

    private EmailAddress() {
    }

    public static String normalize(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email address is required");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (!VALID_EMAIL.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("Email address is invalid");
        }

        return normalizedEmail;
    }
}
