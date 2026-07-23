package com.llenroctech.customerconnect.repository;

import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.dto.UserDTO;
import com.llenroctech.customerconnect.domain.PasswordResetVerification;

import java.util.Collection;

public interface UserRepository<T extends User> {

    T create(T data);

    Collection<T> list(int page, int pageSize);

    T get(Long id);

    T update(T data);

    Boolean delete(Long id);

    T getUserByEmail(String email);

    String createVerificationCode(UserDTO userDTO);

    boolean verifyCode(String email, String code);

    boolean verifyAccount(String key);

    void requestPasswordReset(String email);

    PasswordResetVerification findPasswordResetVerification(String token);

    int updatePassword(Long userId, String encodedPassword);

    int deletePasswordResetToken(Long userId, String token);
}
