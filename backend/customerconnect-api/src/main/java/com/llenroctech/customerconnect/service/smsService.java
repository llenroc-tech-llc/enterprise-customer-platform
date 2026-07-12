package com.llenroctech.customerconnect.service;

public interface smsService {
    void sendVerificationCode(
            String phoneNumber,
            String verificationCode
    );
}
