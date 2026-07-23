package com.llenroctech.customerconnect.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@Slf4j
public class MfaCodeGenerator {

    static final int CODE_LENGTH = 8;
    private static final int CODE_BOUND = 100_000_000;
    private static final String DEVELOPMENT_CODE_PROPERTY = "app.mfa.dev-code";

    private final Environment environment;
    private final SecureRandom secureRandom;

    @Autowired
    public MfaCodeGenerator(Environment environment) {
        this(environment, new SecureRandom());
    }

    MfaCodeGenerator(
            Environment environment,
            SecureRandom secureRandom
    ) {
        this.environment = environment;
        this.secureRandom = secureRandom;
    }

    public String generate() {
        if (usesDevelopmentOverride()) {
            String configuredCode = environment.getProperty(
                    DEVELOPMENT_CODE_PROPERTY
            );
            validateDevelopmentCode(configuredCode);
            log.info("Development MFA code override is configured");
            return configuredCode;
        }

        return String.format(
                "%0" + CODE_LENGTH + "d",
                secureRandom.nextInt(CODE_BOUND)
        );
    }

    private boolean usesDevelopmentOverride() {
        boolean developmentOrTest = environment.acceptsProfiles(
                Profiles.of("dev", "test")
        );
        boolean production = environment.acceptsProfiles(Profiles.of("prod"));
        return developmentOrTest && !production;
    }

    private void validateDevelopmentCode(String configuredCode) {
        if (configuredCode == null || configuredCode.isBlank()) {
            throw new IllegalStateException(
                    "APP_MFA_DEV_CODE must be configured for development MFA"
            );
        }
        if (!configuredCode.matches("\\d{" + CODE_LENGTH + "}")) {
            throw new IllegalStateException(
                    "APP_MFA_DEV_CODE must contain exactly "
                            + CODE_LENGTH + " digits"
            );
        }
    }
}
