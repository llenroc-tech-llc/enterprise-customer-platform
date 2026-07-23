package com.llenroctech.customerconnect.service.implementation;

import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.domain.EmailAddress;
import com.llenroctech.customerconnect.dto.UserDTO;
import com.llenroctech.customerconnect.dto.PasswordResetVerificationResponse;
import com.llenroctech.customerconnect.domain.PasswordResetVerification;
import com.llenroctech.customerconnect.dtomapper.UserDTOMapper;
import com.llenroctech.customerconnect.repository.UserRepository;
import com.llenroctech.customerconnect.service.SmsService;
import com.llenroctech.customerconnect.service.UserService;
import com.llenroctech.customerconnect.request.PasswordResetRequest;
import com.llenroctech.customerconnect.exception.InvalidPasswordResetTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository<User> userRepository;
    private final SmsService smsService;
    private final BCryptPasswordEncoder passwordEncoder;

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
}
