package com.llenroctech.customerconnect.repository.implementation;

import com.llenroctech.customerconnect.domain.Role;
import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.domain.EmailAddress;
import com.llenroctech.customerconnect.domain.PasswordResetVerification;
import com.llenroctech.customerconnect.dto.UserDTO;
import com.llenroctech.customerconnect.exception.UserAlreadyExistsException;
import com.llenroctech.customerconnect.exception.ExpiredPasswordResetTokenException;
import com.llenroctech.customerconnect.exception.InvalidPasswordResetTokenException;
import com.llenroctech.customerconnect.exception.InvalidAccountVerificationException;
import com.llenroctech.customerconnect.repository.RoleRepository;
import com.llenroctech.customerconnect.repository.UserRepository;
import com.llenroctech.customerconnect.rowmapper.UserRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.llenroctech.customerconnect.enumeration.RoleType.ROLE_USER;
import static com.llenroctech.customerconnect.query.UserQuery.*;
import static java.util.Objects.requireNonNull;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepositoryImpl implements UserRepository<User> {

    private static final long VERIFICATION_EXPIRATION_MINUTES = 10;

    private final NamedParameterJdbcTemplate jdbc;
    private final RoleRepository<Role> roleRepository;
    private final BCryptPasswordEncoder encoder;

    @Override
    public User create(User user) {
        if (getEmailCount(user.getEmail().trim().toLowerCase()) > 0) {
            throw new UserAlreadyExistsException(
                    "Email already in use. Please use a different email and try again."
            );
        }

        try {
            KeyHolder holder = new GeneratedKeyHolder();
            SqlParameterSource parameters = getSqlParameterSource(user);

            jdbc.update(INSERT_USER_QUERY, parameters, holder);

            user.setId(requireNonNull(holder.getKey()).longValue());
            roleRepository.addRoleToUser(user.getId(), ROLE_USER.name());
            String verificationUrl = buildAccountVerificationUrl(
                    UUID.randomUUID().toString()
            );

            jdbc.update(INSERT_ACCOUNT_VERIFICATION_URL_QUERY, Map.of("userId", user.getId(), "url", verificationUrl));
            //TODO: Set up email service
            //emailService.sendVerificationUrl(user.getFirstName(), user.getEmail(), verificationUrl, ACCOUNT.getType());
            user.setEnabled(false);
            user.setNotLocked(true);

            return user;
        } catch (DuplicateKeyException exception) {
            log.warn("User creation rejected by a uniqueness constraint");
            throw new UserAlreadyExistsException(
                    "An account with that email already exists."
            );
        } catch (DataIntegrityViolationException exception) {
            log.warn("User creation rejected by a data integrity constraint");
            throw exception;
        } catch (Exception exception) {
            log.error("Error creating user", exception);
            throw new RuntimeException(exception);
        }
    }

    @Override
    public Collection<User> list(int page, int pageSize) {
        return List.of();
    }

    @Override
    public User get(Long id) {
        return null;
    }

    @Override
    public User update(User data) {
        return null;
    }

    @Override
    public Boolean delete(Long id) {
        return false;
    }

    @Override
    public User getUserByEmail(String email) {
        try {
            return jdbc.queryForObject(
                    SELECT_USER_BY_EMAIL_QUERY,
                    Map.of("email", email.trim().toLowerCase()),
                    new UserRowMapper()
            );
        } catch (EmptyResultDataAccessException exception) {
            return null;
        } catch (Exception exception) {
            log.error("Error retrieving user by normalized account identifier", exception);
            throw exception;
        }
    }

    @Override
    @Transactional
    public String createVerificationCode(
            UserDTO user,
            String verificationCode
    ) {
        LocalDateTime expirationDate = verificationExpirationDate();

        try {
            jdbc.update(
                    DELETE_VERIFICATION_CODE_BY_USER_ID,
                    Map.of("id", user.getId())
            );

            MapSqlParameterSource parameters =
                    new MapSqlParameterSource()
                            .addValue("userId", user.getId())
                            .addValue("code", verificationCode)
                            .addValue("expirationDate", expirationDate);

            jdbc.update(
                    INSERT_VERIFICATION_CODE_QUERY,
                    parameters
            );

            return verificationCode;

        } catch (Exception exception) {
            log.error(
                    "Failed to create verification code for user ID {}",
                    user.getId(),
                    exception
            );

            throw new RuntimeException(
                    "An error occurred while creating the verification code.",
                    exception
            );
        }
    }

    @Override
    @Transactional
    public boolean verifyCode(String email, String code) {
        int deleted = jdbc.update(
                CONSUME_VALID_VERIFICATION_CODE_QUERY,
                Map.of(
                        "email", email.trim().toLowerCase(),
                        "code", code.trim()
                )
        );
        return deleted == 1;
    }

    @Override
    public boolean verifyAccount(String key) {
        String verificationUrl = buildAccountVerificationUrl(
                requireVerificationKey(key)
        );

        try {
            AccountVerification accountVerification = jdbc.queryForObject(
                    SELECT_ACCOUNT_VERIFICATION_QUERY,
                    Map.of("url", verificationUrl),
                    (resultSet, rowNumber) -> new AccountVerification(
                            resultSet.getLong("user_id"),
                            resultSet.getBoolean("enabled")
                    )
            );
            if (accountVerification.alreadyVerified()) {
                return true;
            }

            int updated = jdbc.update(
                    ENABLE_VERIFIED_ACCOUNT_QUERY,
                    Map.of("userId", accountVerification.userId())
            );
            if (updated != 1) {
                throw new IllegalStateException(
                        "Account verification update failed"
                );
            }
            return false;
        } catch (EmptyResultDataAccessException exception) {
            throw new InvalidAccountVerificationException(
                    "Account verification record was not found"
            );
        }
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        String normalizedEmail = EmailAddress.normalize(email);

        if (getEmailCount(normalizedEmail) == 0) {
            log.debug("Password reset requested for an unknown account");
            return;
        }

        User user = getUserByEmail(normalizedEmail);
        if (user == null) {
            log.debug("Password reset account was not available after lookup");
            return;
        }

        String resetUrl = buildPasswordResetUrl(
                UUID.randomUUID().toString()
        );
        LocalDateTime expirationDate = verificationExpirationDate();

        try {
            jdbc.update(
                    DELETE_PASSWORD_RESET_BY_USER_ID_QUERY,
                    Map.of("userId", user.getId())
            );
            jdbc.update(
                    INSERT_PASSWORD_RESET_QUERY,
                    new MapSqlParameterSource()
                            .addValue("userId", user.getId())
                            .addValue("url", resetUrl)
                            .addValue("expirationDate", expirationDate)
            );
        } catch (DataAccessException exception) {
            log.error(
                    "Failed to persist password reset request for user ID {}",
                    user.getId(),
                    exception
            );
            throw exception;
        }
    }

    @Override
    public PasswordResetVerification findPasswordResetVerification(
            String token
    ) {
        String normalizedToken = normalizeResetToken(token);
        String resetUrl = buildPasswordResetUrl(normalizedToken);

        try {
            PasswordResetVerification verification = jdbc.queryForObject(
                    SELECT_PASSWORD_RESET_VERIFICATION_QUERY,
                    Map.of("url", resetUrl),
                    (resultSet, rowNumber) -> {
                        if (!resultSet.getBoolean("enabled")
                                || !resultSet.getBoolean("non_locked")) {
                            throw new InvalidPasswordResetTokenException(
                                    "Password reset account is unavailable"
                            );
                        }
                        return new PasswordResetVerification(
                                resultSet.getLong("user_id"),
                                resultSet.getTimestamp("expiration_date")
                                        .toLocalDateTime()
                        );
                    }
            );

            if (!verification.expirationDate().isAfter(LocalDateTime.now())) {
                throw new ExpiredPasswordResetTokenException(
                        "Password reset token has expired"
                );
            }
            return verification;
        } catch (EmptyResultDataAccessException exception) {
            throw new InvalidPasswordResetTokenException(
                    "Password reset token was not found"
            );
        }
    }

    @Override
    public int updatePassword(Long userId, String encodedPassword) {
        return jdbc.update(
                UPDATE_USER_PASSWORD_QUERY,
                Map.of(
                        "userId", userId,
                        "password", encodedPassword
                )
        );
    }

    @Override
    public int deletePasswordResetToken(Long userId, String token) {
        return jdbc.update(
                DELETE_PASSWORD_RESET_TOKEN_QUERY,
                Map.of(
                        "userId", userId,
                        "url", buildPasswordResetUrl(normalizeResetToken(token))
                )
        );
    }

    private Integer getEmailCount(String email) {
        return jdbc.queryForObject(COUNT_USER_EMAIL_QUERY, Map.of("email", email), Integer.class);
    }

    private SqlParameterSource getSqlParameterSource(User user) {
        return new MapSqlParameterSource()
                .addValue("firstName", user.getFirstName())
                .addValue("lastName", user.getLastName())
                .addValue("email", user.getEmail().trim().toLowerCase())
                .addValue("password", encoder.encode(user.getPassword()));
    }

    /*
     * Verification records are retained for course-compatible audit analytics.
     * Production hardening should store a SHA-256 token hash, not the raw URL.
     */
    private String buildAccountVerificationUrl(String key) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/user/verify/account/{key}")
                .buildAndExpand(key)
                .toUriString();
    }

    private String requireVerificationKey(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidAccountVerificationException(
                    "Account verification key is required"
            );
        }
        return key.trim();
    }

    /*
     * Course-compatible storage uses the complete URL. Production hardening
     * should store only a SHA-256 token hash and compare token hashes instead.
     */
    private String buildPasswordResetUrl(String token) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/user/verify/password/{token}")
                .buildAndExpand(token)
                .toUriString();
    }

    private String normalizeResetToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidPasswordResetTokenException(
                    "Password reset token is required"
            );
        }
        return token.trim();
    }

    private LocalDateTime verificationExpirationDate() {
        return LocalDateTime.now().plusMinutes(
                VERIFICATION_EXPIRATION_MINUTES
        );
    }

    private record AccountVerification(
            long userId,
            boolean alreadyVerified
    ) {
    }
}
