package com.llenroctech.customerconnect.service;

public interface SmsService {
    void sendVerificationCode(
            String phoneNumber,
            String verificationCode
    );
}
