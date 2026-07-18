package com.llenroctech.customerconnect.service.implementation;

import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.dto.UserDTO;
import com.llenroctech.customerconnect.repository.UserRepository;
import com.llenroctech.customerconnect.service.SmsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class UserServiceImplTest {

    private static final String VERIFICATION_CODE = "48392157";

    @Test
    void logsVerificationCodeAndOnlyLastFourPhoneDigitsInDev(CapturedOutput output) {
        UserRepository<User> userRepository = mock(UserRepository.class);
        SmsService smsService = mock(SmsService.class);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        UserDTO user = userWithPhone("+1 (555) 555-5262");
        when(userRepository.createVerificationCode(user)).thenReturn(VERIFICATION_CODE);

        new UserServiceImpl(userRepository, smsService, environment)
                .sendVerificationCode(user);

        assertThat(output).contains(
                "Development MFA code for phone ending in 5262: 48392157"
        );
        assertThat(output).doesNotContain("+1 (555) 555-5262");
        verify(smsService).sendVerificationCode(user.getPhone(), VERIFICATION_CODE);
    }

    @Test
    void doesNotLogVerificationCodeOutsideDev(CapturedOutput output) {
        UserRepository<User> userRepository = mock(UserRepository.class);
        SmsService smsService = mock(SmsService.class);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        UserDTO user = userWithPhone("555-555-5262");
        when(userRepository.createVerificationCode(user)).thenReturn(VERIFICATION_CODE);

        new UserServiceImpl(userRepository, smsService, environment)
                .sendVerificationCode(user);

        assertThat(output).doesNotContain(VERIFICATION_CODE);
        verify(smsService).sendVerificationCode(user.getPhone(), VERIFICATION_CODE);
    }

    private UserDTO userWithPhone(String phoneNumber) {
        UserDTO user = new UserDTO();
        user.setPhone(phoneNumber);
        return user;
    }
}
