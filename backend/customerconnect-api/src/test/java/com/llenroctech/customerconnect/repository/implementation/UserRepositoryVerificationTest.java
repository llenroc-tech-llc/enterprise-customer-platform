package com.llenroctech.customerconnect.repository.implementation;

import com.llenroctech.customerconnect.domain.Role;
import com.llenroctech.customerconnect.domain.PasswordResetVerification;
import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.exception.ExpiredPasswordResetTokenException;
import com.llenroctech.customerconnect.exception.InvalidPasswordResetTokenException;
import com.llenroctech.customerconnect.exception.UserAlreadyExistsException;
import com.llenroctech.customerconnect.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static com.llenroctech.customerconnect.query.UserQuery.CONSUME_VALID_VERIFICATION_CODE_QUERY;
import static com.llenroctech.customerconnect.query.UserQuery.COUNT_USER_EMAIL_QUERY;
import static com.llenroctech.customerconnect.query.UserQuery.INSERT_USER_QUERY;
import static com.llenroctech.customerconnect.query.UserQuery.DELETE_PASSWORD_RESET_BY_USER_ID_QUERY;
import static com.llenroctech.customerconnect.query.UserQuery.INSERT_PASSWORD_RESET_QUERY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.sql.ResultSet;
import java.sql.Timestamp;

import static com.llenroctech.customerconnect.query.UserQuery.DELETE_PASSWORD_RESET_TOKEN_QUERY;
import static com.llenroctech.customerconnect.query.UserQuery.SELECT_PASSWORD_RESET_VERIFICATION_QUERY;
import static com.llenroctech.customerconnect.query.UserQuery.UPDATE_USER_PASSWORD_QUERY;

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

    @BeforeEach
    void setRequestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("customerconnect.example.com");
        request.setServerPort(443);
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(request)
        );
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

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

    @Test
    void passwordResetDeletesPreviousRequestBeforeInsertingNewOne() {
        UserRepositoryImpl resetRepository = spy(repository);
        User user = resetUser();
        when(jdbc.queryForObject(
                eq(COUNT_USER_EMAIL_QUERY),
                anyMap(),
                eq(Integer.class)
        )).thenReturn(1);
        doReturn(user).when(resetRepository)
                .getUserByEmail("user@example.com");

        resetRepository.requestPasswordReset("  User@EXAMPLE.COM ");

        InOrder ordered = inOrder(jdbc);
        ordered.verify(jdbc).update(
                eq(DELETE_PASSWORD_RESET_BY_USER_ID_QUERY),
                eq(java.util.Map.of("userId", 42L))
        );
        ArgumentCaptor<SqlParameterSource> parameters =
                ArgumentCaptor.forClass(SqlParameterSource.class);
        ordered.verify(jdbc).update(
                eq(INSERT_PASSWORD_RESET_QUERY),
                parameters.capture()
        );

        assertThat(parameters.getValue().getValue("userId")).isEqualTo(42L);
        assertThat(parameters.getValue().getValue("url").toString())
                .startsWith(
                        "https://customerconnect.example.com/user/verify/password/"
                );
        assertThat((LocalDateTime) parameters.getValue()
                .getValue("expirationDate"))
                .isAfter(LocalDateTime.now().plusMinutes(9))
                .isBefore(LocalDateTime.now().plusMinutes(11));
        verify(resetRepository).getUserByEmail("user@example.com");
    }

    @Test
    void repeatedResetRequestsReplaceTheActiveRequest() {
        UserRepositoryImpl resetRepository = spy(repository);
        when(jdbc.queryForObject(
                eq(COUNT_USER_EMAIL_QUERY),
                anyMap(),
                eq(Integer.class)
        )).thenReturn(1);
        doReturn(resetUser()).when(resetRepository)
                .getUserByEmail("user@example.com");

        resetRepository.requestPasswordReset("user@example.com");
        resetRepository.requestPasswordReset("user@example.com");

        verify(jdbc, times(2)).update(
                eq(DELETE_PASSWORD_RESET_BY_USER_ID_QUERY),
                anyMap()
        );
        verify(jdbc, times(2)).update(
                eq(INSERT_PASSWORD_RESET_QUERY),
                any(SqlParameterSource.class)
        );
    }

    @Test
    void unknownAccountDoesNotCreateResetRequest() {
        when(jdbc.queryForObject(
                eq(COUNT_USER_EMAIL_QUERY),
                anyMap(),
                eq(Integer.class)
        )).thenReturn(0);

        repository.requestPasswordReset("unknown@example.com");

        verify(jdbc, never()).update(
                eq(DELETE_PASSWORD_RESET_BY_USER_ID_QUERY),
                anyMap()
        );
        verify(jdbc, never()).update(
                eq(INSERT_PASSWORD_RESET_QUERY),
                any(SqlParameterSource.class)
        );
    }

    @Test
    void databaseFailureIsLoggedAndPropagatedWithoutInsert() {
        UserRepositoryImpl resetRepository = spy(repository);
        when(jdbc.queryForObject(
                eq(COUNT_USER_EMAIL_QUERY),
                anyMap(),
                eq(Integer.class)
        )).thenReturn(1);
        doReturn(resetUser()).when(resetRepository)
                .getUserByEmail("user@example.com");
        DataAccessResourceFailureException failure =
                new DataAccessResourceFailureException("database unavailable");
        when(jdbc.update(
                eq(DELETE_PASSWORD_RESET_BY_USER_ID_QUERY),
                anyMap()
        )).thenThrow(failure);

        assertThatThrownBy(
                () -> resetRepository.requestPasswordReset("user@example.com")
        ).isSameAs(failure);
        verify(jdbc, never()).update(
                eq(INSERT_PASSWORD_RESET_QUERY),
                any(SqlParameterSource.class)
        );
    }

    @Test
    void activeResetTokenResolvesUserWithoutConsumingTheRecord()
            throws Exception {
        ResultSet resultSet = activeResetResult(
                LocalDateTime.now().plusMinutes(5)
        );
        when(jdbc.queryForObject(
                eq(SELECT_PASSWORD_RESET_VERIFICATION_QUERY),
                anyMap(),
                any(RowMapper.class)
        )).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            RowMapper<PasswordResetVerification> mapper =
                    invocation.getArgument(2);
            return mapper.mapRow(resultSet, 0);
        });

        PasswordResetVerification result =
                repository.findPasswordResetVerification("safe-token");

        assertThat(result.userId()).isEqualTo(42L);
        verify(jdbc, never()).update(
                eq(DELETE_PASSWORD_RESET_TOKEN_QUERY),
                anyMap()
        );
    }

    @Test
    void expiredResetTokenIsRejected() throws Exception {
        ResultSet resultSet = activeResetResult(
                LocalDateTime.now().minusSeconds(1)
        );
        when(jdbc.queryForObject(
                eq(SELECT_PASSWORD_RESET_VERIFICATION_QUERY),
                anyMap(),
                any(RowMapper.class)
        )).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            RowMapper<PasswordResetVerification> mapper =
                    invocation.getArgument(2);
            return mapper.mapRow(resultSet, 0);
        });

        assertThatThrownBy(
                () -> repository.findPasswordResetVerification("safe-token")
        ).isInstanceOf(ExpiredPasswordResetTokenException.class);
    }

    @Test
    void unknownResetTokenIsRejected() {
        when(jdbc.queryForObject(
                eq(SELECT_PASSWORD_RESET_VERIFICATION_QUERY),
                anyMap(),
                any(RowMapper.class)
        )).thenThrow(new org.springframework.dao.EmptyResultDataAccessException(
                1
        ));

        assertThatThrownBy(
                () -> repository.findPasswordResetVerification("unknown")
        ).isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    @Test
    void passwordUpdateAndTokenDeletionUseScopedNamedParameters() {
        when(jdbc.update(eq(UPDATE_USER_PASSWORD_QUERY), anyMap()))
                .thenReturn(1);
        when(jdbc.update(eq(DELETE_PASSWORD_RESET_TOKEN_QUERY), anyMap()))
                .thenReturn(1);

        assertThat(repository.updatePassword(42L, "bcrypt-value"))
                .isEqualTo(1);
        assertThat(repository.deletePasswordResetToken(42L, "safe-token"))
                .isEqualTo(1);
    }

    private User resetUser() {
        return User.builder()
                .id(42L)
                .email("user@example.com")
                .build();
    }

    private ResultSet activeResetResult(LocalDateTime expiration)
            throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("user_id")).thenReturn(42L);
        when(resultSet.getTimestamp("expiration_date"))
                .thenReturn(Timestamp.valueOf(expiration));
        when(resultSet.getBoolean("enabled")).thenReturn(true);
        when(resultSet.getBoolean("non_locked")).thenReturn(true);
        return resultSet;
    }

    @SuppressWarnings("unchecked")
    private RoleRepository<Role> mockRoleRepository() {
        return mock(RoleRepository.class);
    }
}
