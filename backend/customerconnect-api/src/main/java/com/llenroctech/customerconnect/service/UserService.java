package com.llenroctech.customerconnect.service;

import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.dto.UserDTO;
import com.llenroctech.customerconnect.dto.PasswordResetVerificationResponse;
import com.llenroctech.customerconnect.request.PasswordResetRequest;

public interface UserService {
    UserDTO createUser(User user);
    UserDTO getUserByEmail(String email);

    void sendVerificationCode(UserDTO userDTO);

    boolean verifyCode(String email, String code);

    void requestPasswordReset(String email);

    PasswordResetVerificationResponse verifyPasswordResetToken(String token);

    void resetPassword(PasswordResetRequest request);
}
