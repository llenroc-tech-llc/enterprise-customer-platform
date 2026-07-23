package com.llenroctech.customerconnect.service.implementation;

import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.domain.EmailAddress;
import com.llenroctech.customerconnect.dto.UserDTO;
import com.llenroctech.customerconnect.dto.PasswordResetVerificationResponse;
import com.llenroctech.customerconnect.domain.PasswordResetVerification;
import com.llenroctech.customerconnect.domain.RefreshedTokens;
import com.llenroctech.customerconnect.dto.AccountVerificationResult;
import com.llenroctech.customerconnect.dtomapper.UserDTOMapper;
import com.llenroctech.customerconnect.repository.UserRepository;
import com.llenroctech.customerconnect.service.SmsService;
import com.llenroctech.customerconnect.service.UserService;
import com.llenroctech.customerconnect.request.PasswordResetRequest;
import com.llenroctech.customerconnect.exception.InvalidPasswordResetTokenException;
import com.llenroctech.customerconnect.exception.InvalidAccountVerificationException;
import com.llenroctech.customerconnect.provider.TokenProvider;
import com.llenroctech.customerconnect.security.model.CustomerConnectUserPrincipal;
import com.auth0.jwt.exceptions.JWTVerificationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository<User> userRepository;
    private final SmsService smsService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    @Transactional
    public UserDTO createUser(User user) {
        return UserDTOMapper.fromUser(
                userRepository.create(user)
        );
    }

    @Override
    public UserDTO getUserByEmail(String email) {
        return UserDTOMapper.fromUser(
                userRepository.getUserByEmail(email)
        );
    }

    @Override
    @Transactional
    public void sendVerificationCode(UserDTO userDTO) {
        String verificationCode =
                userRepository.createVerificationCode(userDTO);

        smsService.sendVerificationCode(
                userDTO.getPhone(),
                verificationCode
        );
    }

    @Override
    public boolean verifyCode(String email, String code) {
        return userRepository.verifyCode(email, code);
    }

    @Override
    @Transactional
    public AccountVerificationResult verifyAccount(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidAccountVerificationException(
                    "Account verification key is required"
            );
        }
        return new AccountVerificationResult(
                userRepository.verifyAccount(key.trim())
        );
    }

    @Override
    public RefreshedTokens refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadCredentialsException("Refresh token is required");
        }

        try {
            String subject = tokenProvider.verifyRefreshToken(refreshToken)
                    .getSubject();
            if (subject == null || subject.isBlank()) {
                throw new BadCredentialsException(
                        "Refresh token subject is invalid"
                );
            }

            CustomerConnectUserPrincipal principal = requirePrincipal(
                    userDetailsService.loadUserByUsername(subject)
            );
            validateAccountStatus(principal);
            return new RefreshedTokens(
                    tokenProvider.createAccessToken(principal),
                    tokenProvider.createRefreshToken(principal)
            );
        } catch (JWTVerificationException exception) {
            throw new BadCredentialsException(
                    "Refresh token is invalid",
                    exception
            );
        }
    }

    @Override
    public void requestPasswordReset(String email) {
        String normalizedEmail = EmailAddress.normalize(email);
        userRepository.requestPasswordReset(normalizedEmail);

        // TODO Introduce PasswordResetNotificationService when email delivery
        // is implemented. Tokens and reset URLs must remain outside API responses.
    }

    @Override
    public PasswordResetVerificationResponse verifyPasswordResetToken(
            String token
    ) {
        String normalizedToken = requireResetToken(token);
        userRepository.findPasswordResetVerification(normalizedToken);
        return new PasswordResetVerificationResponse(true);
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Password reset request is required");
        }

        String token = requireResetToken(request.getToken());
        String password = request.getPassword();
        String confirmation = request.getConfirmPassword();

        if (password == null || password.isBlank()
                || confirmation == null || confirmation.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (!password.equals(confirmation)) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        PasswordResetVerification verification =
                userRepository.findPasswordResetVerification(token);
        String encodedPassword = passwordEncoder.encode(password);

        if (userRepository.updatePassword(
                verification.userId(),
                encodedPassword
        ) != 1) {
            throw new IllegalStateException("Password update did not succeed");
        }
        if (userRepository.deletePasswordResetToken(
                verification.userId(),
                token
        ) != 1) {
            throw new IllegalStateException(
                    "Password reset token invalidation did not succeed"
            );
        }
    }

    private String requireResetToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidPasswordResetTokenException(
                    "Password reset token is required"
            );
        }
        return token.trim();
    }

    private CustomerConnectUserPrincipal requirePrincipal(
            UserDetails userDetails
    ) {
        if (userDetails instanceof CustomerConnectUserPrincipal principal) {
            return principal;
        }
        throw new BadCredentialsException("Authenticated user is invalid");
    }

    private void validateAccountStatus(UserDetails userDetails) {
        if (!userDetails.isEnabled()
                || !userDetails.isAccountNonLocked()
                || !userDetails.isAccountNonExpired()
                || !userDetails.isCredentialsNonExpired()) {
            throw new BadCredentialsException("User account is unavailable");
        }
    }
}
