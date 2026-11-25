package com.pragma.powerup.domain.exception;

public class OrderNotFromEmployeeRestaurantException extends DomainException {
    public OrderNotFromEmployeeRestaurantException(String message) {
        super(message);
    }
}
