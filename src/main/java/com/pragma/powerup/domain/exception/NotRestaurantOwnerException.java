package com.pragma.powerup.domain.exception;

public class NotRestaurantOwnerException extends DomainException {
    public NotRestaurantOwnerException(String message) {
        super(message);
    }
}
