package com.pragma.powerup.domain.exception;

public class EmployeeAlreadyAssignedException extends DomainException {
    public EmployeeAlreadyAssignedException(String message) {
        super(message);
    }
}
