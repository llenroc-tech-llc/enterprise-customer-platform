package com.llenroctech.customerconnect.repository.implementation;

import com.llenroctech.customerconnect.domain.Role;
import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.exception.UserAlreadyExistsException;
import com.llenroctech.customerconnect.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;

import static com.llenroctech.customerconnect.query.UserQuery.CONSUME_VALID_VERIFICATION_CODE_QUERY;
import static com.llenroctech.customerconnect.query.UserQuery.COUNT_USER_EMAIL_QUERY;
import static com.llenroctech.customerconnect.query.UserQuery.INSERT_USER_QUERY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserRepositoryVerificationTest {

    private static final String EMAIL = "cornell@example.com";
    private static final String CODE = "48392157";

    private final NamedParameterJdbcTemplate jdbc =
            mock(NamedParameterJdbcTemplate.class);
    private final UserRepositoryImpl repository = new UserRepositoryImpl(
            jdbc,
            mockRoleRepository(),
            mock(BCryptPasswordEncoder.class)
    );

    @Test
    void validUnexpiredCodeIsConsumedSuccessfully() {
        when(jdbc.update(eq(CONSUME_VALID_VERIFICATION_CODE_QUERY), anyMap()))
                .thenReturn(1);

        assertThat(repository.verifyCode(EMAIL, CODE))
                .isTrue();
        assertThat(CONSUME_VALID_VERIFICATION_CODE_QUERY)
                .contains("user.email = :email")
                .contains("verification.code = :code")
                .contains("verification.expiration_date > NOW()")
                .startsWith("DELETE verification");
    }

    @Test
    void expiredCodeFailsAtomicConsumption() {
        when(jdbc.update(eq(CONSUME_VALID_VERIFICATION_CODE_QUERY), anyMap()))
                .thenReturn(0);

        assertThat(repository.verifyCode(EMAIL, CODE))
                .isFalse();
    }

    @Test
    void invalidCodeFailsAtomicConsumption() {
        when(jdbc.update(eq(CONSUME_VALID_VERIFICATION_CODE_QUERY), anyMap()))
                .thenReturn(0);

        assertThat(repository.verifyCode(EMAIL, "00000000"))
                .isFalse();
    }

    @Test
    void consumedCodeCannotBeReused() {
        when(jdbc.update(eq(CONSUME_VALID_VERIFICATION_CODE_QUERY), anyMap()))
                .thenReturn(1, 0);

        assertThat(repository.verifyCode(EMAIL, CODE))
                .isTrue();
        assertThat(repository.verifyCode(EMAIL, CODE))
                .isFalse();
    }

    @Test
    void databaseDuplicateIsMappedToDomainConflict() {
        when(jdbc.queryForObject(
                eq(COUNT_USER_EMAIL_QUERY),
                anyMap(),
                eq(Integer.class)
        )).thenReturn(0);
        when(jdbc.update(
                eq(INSERT_USER_QUERY),
                any(SqlParameterSource.class),
                any(KeyHolder.class)
        )).thenThrow(new DuplicateKeyException(
                "Duplicate entry; users_email_idx"
        ));

        User user = User.builder()
                .firstName("Test")
                .lastName("User")
                .email(EMAIL)
                .password("Password123!")
                .build();

        assertThatThrownBy(() -> repository.create(user))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("An account with that email already exists.");
    }

    @SuppressWarnings("unchecked")
    private RoleRepository<Role> mockRoleRepository() {
        return mock(RoleRepository.class);
    }
}
