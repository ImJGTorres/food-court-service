package com.pragma.powerup.domain.exception;

public class OrderNotPendingException extends DomainException {
    public OrderNotPendingException(String message) {
        super(message);
    }
}
