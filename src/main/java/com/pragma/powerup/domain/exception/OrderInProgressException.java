package com.pragma.powerup.domain.exception;

public class OrderInProgressException extends DomainException {
    public OrderInProgressException(String message) {
        super(message);
    }
}
