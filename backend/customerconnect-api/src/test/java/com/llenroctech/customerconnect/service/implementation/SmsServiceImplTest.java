package com.llenroctech.customerconnect.service.implementation;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.env.MockEnvironment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class SmsServiceImplTest {

    @Test
    void developmentSkipsTwilioInitializationAndDelivery() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        SmsServiceImpl service = new SmsServiceImpl(environment);

        try (MockedStatic<Twilio> twilio = mockStatic(Twilio.class);
             MockedStatic<Message> message = mockStatic(Message.class)) {
            service.initializeTwilio();
            service.sendVerificationCode(
                    "+1 (555) 555-5262",
                    "48392157"
            );

            twilio.verifyNoInteractions();
            message.verifyNoInteractions();
        }
    }

    @Test
    void productionInitializesTwilioAndUsesRealMessageAdapter() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("twilio.account-sid", "account-sid")
                .withProperty("twilio.auth-token", "auth-token")
                .withProperty("twilio.phone-number", "+15555550000");
        environment.setActiveProfiles("prod");
        SmsServiceImpl service = new SmsServiceImpl(environment);
        MessageCreator creator = mock(MessageCreator.class);
        Message sentMessage = mock(Message.class);

        try (MockedStatic<Twilio> twilio = mockStatic(Twilio.class);
             MockedStatic<Message> message = mockStatic(Message.class)) {
            message.when(() -> Message.creator(
                    any(PhoneNumber.class),
                    any(PhoneNumber.class),
                    anyString()
            )).thenReturn(creator);
            org.mockito.Mockito.when(creator.create()).thenReturn(sentMessage);
            org.mockito.Mockito.when(sentMessage.getSid()).thenReturn("SM-test");

            service.initializeTwilio();
            service.sendVerificationCode(
                    "+1 (555) 555-5262",
                    "48392157"
            );

            twilio.verify(() -> Twilio.init("account-sid", "auth-token"));
            verify(creator).create();
        }
    }
}
