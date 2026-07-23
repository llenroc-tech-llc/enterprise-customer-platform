package com.llenroctech.customerconnect.security;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfaCodeGeneratorTest {

    @Test
    void springCanInjectGeneratorThroughItsExplicitApplicationConstructor() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {
            context.registerBean(Environment.class, () -> environment);
            context.register(MfaCodeGenerator.class);
            context.refresh();

            assertThat(context.getBean(MfaCodeGenerator.class)).isNotNull();
        }
    }

    @Test
    void developmentUsesConfiguredEightDigitCode() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.mfa.dev-code", "48392157");
        environment.setActiveProfiles("dev");

        assertThat(new MfaCodeGenerator(environment).generate())
                .isEqualTo("48392157");
    }

    @Test
    void developmentRejectsMissingOrInvalidOverride() {
        MockEnvironment missing = new MockEnvironment();
        missing.setActiveProfiles("dev");
        MockEnvironment invalid = new MockEnvironment()
                .withProperty("app.mfa.dev-code", "1234-ab");
        invalid.setActiveProfiles("test");

        assertThatThrownBy(() -> new MfaCodeGenerator(missing).generate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_MFA_DEV_CODE");
        assertThatThrownBy(() -> new MfaCodeGenerator(invalid).generate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("8 digits");
    }

    @Test
    void productionCannotActivateDevelopmentOverride() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.mfa.dev-code", "48392157");
        environment.setActiveProfiles("dev", "prod");
        SecureRandom secureRandom = mock(SecureRandom.class);
        when(secureRandom.nextInt(anyInt())).thenReturn(42);

        String generated = new MfaCodeGenerator(
                environment,
                secureRandom
        ).generate();

        assertThat(generated).isEqualTo("00000042");
        verify(secureRandom).nextInt(100_000_000);
    }

    @Test
    void productionGeneratesEightDigitCode() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThat(new MfaCodeGenerator(environment).generate())
                .matches("\\d{8}");
    }
}
