package com.yandex.reactive.testcontainers.reshop.exception;

public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}
