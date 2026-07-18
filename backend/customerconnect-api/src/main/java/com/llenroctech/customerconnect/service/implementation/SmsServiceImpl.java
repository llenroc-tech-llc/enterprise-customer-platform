package com.llenroctech.customerconnect.service.implementation;

import com.llenroctech.customerconnect.service.SmsService;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;



@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromPhoneNumber;

    @PostConstruct
    void initializeTwilio() {
        Twilio.init(accountSid, authToken);
        log.info("Twilio SMS client initialized");
    }

    @Override
    public void sendVerificationCode(
            String phoneNumber,
            String verificationCode
    ) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(formatPhoneNumber(phoneNumber)),
                    new PhoneNumber(fromPhoneNumber),
                    buildVerificationMessage(verificationCode)
            ).create();

            log.info(
                    "Verification SMS submitted successfully. Message SID: {}, recipient: {}",
                    message.getSid(),
                    maskPhoneNumber(phoneNumber)
            );

        } catch (ApiException exception) {
            log.error(
                    "Twilio failed to send verification SMS to recipient ending in {}",
                    maskPhoneNumber(phoneNumber),
                    exception
            );

            throw new IllegalStateException(
                    "Unable to send the verification code.",
                    exception
            );
        }
    }

    private String buildVerificationMessage(String verificationCode) {
        return """
            CustomerConnect verification code: %s

            This code expires in 10 minutes. Do not share this code.
            """.formatted(verificationCode);
    }

    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "A phone number is required to send a verification code."
            );
        }

        String digitsOnly = phoneNumber.replaceAll("\\D", "");

        if (phoneNumber.startsWith("+")) {
            return "+" + digitsOnly;
        }

        if (digitsOnly.length() == 10) {
            return "+1" + digitsOnly;
        }

        if (digitsOnly.length() == 11 && digitsOnly.startsWith("1")) {
            return "+" + digitsOnly;
        }

        throw new IllegalArgumentException(
                "Phone number must be a valid US phone number."
        );
    }

    private String maskPhoneNumber(String phoneNumber) {
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