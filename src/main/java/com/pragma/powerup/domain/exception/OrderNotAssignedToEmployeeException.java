package com.pragma.powerup.domain.exception;

public class OrderNotAssignedToEmployeeException extends DomainException {
    public OrderNotAssignedToEmployeeException(String message) {
        super(message);
    }
}
