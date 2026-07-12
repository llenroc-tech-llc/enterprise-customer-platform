package com.llenroctech.customerconnect.service.implementation;


import com.llenroctech.customerconnect.service.smsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsServiceImpl implements smsService {

    @Override
    public void sendVerificationCode(
            String phoneNumber,
            String verificationCode
    ) {
        log.info(
                "Development SMS sent to phone ending in {}",
                maskPhoneNumber(phoneNumber)
        );

        log.debug(
                "Development verification code: {}",
                verificationCode
        );
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }

        return phoneNumber.substring(phoneNumber.length() - 4);
    }
}
