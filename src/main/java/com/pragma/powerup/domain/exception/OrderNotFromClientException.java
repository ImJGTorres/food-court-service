package com.pragma.powerup.domain.exception;

public class OrderNotFromClientException extends DomainException {
    public OrderNotFromClientException(String message) {
        super(message);
    }
}
