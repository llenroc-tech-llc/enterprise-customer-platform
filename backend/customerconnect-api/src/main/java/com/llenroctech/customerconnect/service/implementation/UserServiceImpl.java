package com.llenroctech.customerconnect.service.implementation;

import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.dto.UserDTO;
import com.llenroctech.customerconnect.dtomapper.UserDTOMapper;
import com.llenroctech.customerconnect.repository.UserRepository;
import com.llenroctech.customerconnect.service.SmsService;
import com.llenroctech.customerconnect.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository<User> userRepository;
    private final SmsService smsService;
    private final Environment environment;

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

        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            log.info(
                    "Development MFA code for phone ending in {}: {}",
                    getLastFourDigits(userDTO.getPhone()),
                    verificationCode
            );
        }

        smsService.sendVerificationCode(
                userDTO.getPhone(),
                verificationCode
        );
    }

    private String getLastFourDigits(String phoneNumber) {
        if (phoneNumber == null) {
            return "****";
        }

        String digitsOnly = phoneNumber.replaceAll("\\D", "");
        if (digitsOnly.length() < 4) {
            return "****";
        }

        return digitsOnly.substring(digitsOnly.length() - 4);
    }
}
