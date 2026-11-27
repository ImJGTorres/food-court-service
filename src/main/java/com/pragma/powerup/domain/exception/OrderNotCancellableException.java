package com.pragma.powerup.domain.exception;

public class OrderNotCancellableException extends DomainException {
    public OrderNotCancellableException(String message) {
        super(message);
    }
}
