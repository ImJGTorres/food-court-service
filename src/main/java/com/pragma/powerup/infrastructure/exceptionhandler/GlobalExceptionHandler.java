package com.pragma.powerup.infrastructure.exceptionhandler;

import com.pragma.powerup.domain.exception.CategoryNotFoundException;
import com.pragma.powerup.domain.exception.DishAlreadyExistsException;
import com.pragma.powerup.domain.exception.InvalidOwnerException;
import com.pragma.powerup.domain.exception.InvalidRestaurantNameException;
import com.pragma.powerup.domain.exception.NotRestaurantOwnerException;
import com.pragma.powerup.domain.exception.RestaurantAlreadyExistsException;
import com.pragma.powerup.domain.exception.RestaurantNotFoundException;
import com.pragma.powerup.domain.exception.UserServiceUnavailableException;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String MESSAGE = "message";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler({InvalidRestaurantNameException.class, InvalidOwnerException.class})
    public ResponseEntity<Map<String, String>> handleBadRequestException(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap(MESSAGE, exception.getMessage()));
    }

    @ExceptionHandler(RestaurantAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleRestaurantAlreadyExistsException(
            RestaurantAlreadyExistsException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap(MESSAGE, exception.getMessage()));
    }

    @ExceptionHandler(DishAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleDishAlreadyExistsException(
            DishAlreadyExistsException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap(MESSAGE, exception.getMessage()));
    }

    @ExceptionHandler(UserServiceUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleUserServiceUnavailableException(
            UserServiceUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Collections.singletonMap(MESSAGE, exception.getMessage()));
    }

    @ExceptionHandler({RestaurantNotFoundException.class, CategoryNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleNotFoundException(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap(MESSAGE, exception.getMessage()));
    }

    @ExceptionHandler(NotRestaurantOwnerException.class)
    public ResponseEntity<Map<String, String>> handleNotRestaurantOwnerException(NotRestaurantOwnerException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap(MESSAGE, exception.getMessage()));
    }

}
