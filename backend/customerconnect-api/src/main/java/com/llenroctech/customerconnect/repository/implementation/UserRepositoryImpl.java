package com.llenroctech.customerconnect.repository.implementation;

import com.llenroctech.customerconnect.domain.Role;
import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.dto.UserDTO;
import com.llenroctech.customerconnect.exception.UserAlreadyExistsException;
import com.llenroctech.customerconnect.repository.RoleRepository;
import com.llenroctech.customerconnect.repository.UserRepository;
import com.llenroctech.customerconnect.rowmapper.UserRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.llenroctech.customerconnect.enumeration.RoleType.ROLE_USER;
import static com.llenroctech.customerconnect.enumeration.VerificationType.ACCOUNT;
import static com.llenroctech.customerconnect.query.UserQuery.*;
import static java.util.Objects.requireNonNull;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepositoryImpl implements UserRepository<User> {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
            String verificationUrl = buildVerificationUrl(UUID.randomUUID().toString(), ACCOUNT.getType());

            jdbc.update(INSERT_ACCOUNT_VERIFICATION_URL_QUERY, Map.of("userId", user.getId(), "url", verificationUrl));
            //TODO: Set up email service
            //emailService.sendVerificationUrl(user.getFirstName(), user.getEmail(), verificationUrl, ACCOUNT.getType());
            user.setEnabled(false);
            user.setNotLocked(true);

            return user;
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
            log.error("Error retrieving user by email: {}", email, exception);
            throw exception;
        }
    }

    @Override
    @Transactional
    public String createVerificationCode(UserDTO user) {
        LocalDateTime expirationDate =
                LocalDateTime.now().plusMinutes(10);

        String verificationCode =
                String.format(
                        "%08d",
                        SECURE_RANDOM.nextInt(100_000_000)
                );

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

    private String buildVerificationUrl(String key, String type) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/verify/" + type + "/" + key).toUriString();
    }
}
