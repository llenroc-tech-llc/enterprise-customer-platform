package com.llenroctech.customerconnect.service;

import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.dto.UserDTO;

public interface UserService {
    UserDTO createUser(User user);
    UserDTO getUserByEmail(String email);

    void sendVerificationCode(UserDTO userDTO);

    boolean verifyCode(String email, String code);
}
