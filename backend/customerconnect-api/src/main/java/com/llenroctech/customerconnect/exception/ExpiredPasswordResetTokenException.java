package com.llenroctech.customerconnect.exception;

public class ExpiredPasswordResetTokenException extends RuntimeException {

    public ExpiredPasswordResetTokenException(String message) {
        super(message);
    }
}
