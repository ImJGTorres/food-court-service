package com.pragma.powerup.domain.exception;

public class OrderLockedException extends DomainException {
    public OrderLockedException(String message) {
        super(message);
    }
}
