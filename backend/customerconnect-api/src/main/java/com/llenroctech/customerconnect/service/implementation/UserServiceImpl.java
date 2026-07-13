package com.llenroctech.customerconnect.service.implementation;

import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.dto.UserDTO;
import com.llenroctech.customerconnect.dtomapper.UserDTOMapper;
import com.llenroctech.customerconnect.repository.UserRepository;
import com.llenroctech.customerconnect.service.SmsService;
import com.llenroctech.customerconnect.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository<User> userRepository;
    private final SmsService smsService;

    @Override
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
}