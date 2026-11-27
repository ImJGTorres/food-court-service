package com.pragma.powerup.domain.exception;

public class OrderNotReadyException extends DomainException {
    public OrderNotReadyException(String message) {
        super(message);
    }
}
