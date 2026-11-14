package com.pragma.powerup.domain.exception;

public class DishAlreadyExistsException extends DomainException {
    public DishAlreadyExistsException(String message) {
        super(message);
    }
}
