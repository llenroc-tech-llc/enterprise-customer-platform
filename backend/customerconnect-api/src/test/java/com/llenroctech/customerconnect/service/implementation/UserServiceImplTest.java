package com.llenroctech.customerconnect.service.implementation;

import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.dto.UserDTO;
import com.llenroctech.customerconnect.repository.UserRepository;
import com.llenroctech.customerconnect.service.SmsService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private static final String VERIFICATION_CODE = "48392157";

    @Test
    void sendsGeneratedVerificationCode() {
        UserRepository<User> userRepository = mock(UserRepository.class);
        SmsService smsService = mock(SmsService.class);
        UserDTO user = userWithPhone("+1 (555) 555-5262");
        when(userRepository.createVerificationCode(user)).thenReturn(VERIFICATION_CODE);

        new UserServiceImpl(userRepository, smsService)
                .sendVerificationCode(user);

        verify(smsService).sendVerificationCode(user.getPhone(), VERIFICATION_CODE);
    }

    @Test
    void verifiesCodeThroughRepository() {
        UserRepository<User> userRepository = mock(UserRepository.class);
        SmsService smsService = mock(SmsService.class);
        when(userRepository.verifyCode("user@example.com", VERIFICATION_CODE))
                .thenReturn(true);

        boolean verified = new UserServiceImpl(userRepository, smsService)
                .verifyCode("user@example.com", VERIFICATION_CODE);

        org.assertj.core.api.Assertions.assertThat(verified).isTrue();
        verify(userRepository).verifyCode("user@example.com", VERIFICATION_CODE);
    }

    private UserDTO userWithPhone(String phoneNumber) {
        UserDTO user = new UserDTO();
        user.setPhone(phoneNumber);
        return user;
    }
}
