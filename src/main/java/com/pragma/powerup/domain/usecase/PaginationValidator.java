package com.pragma.powerup.domain.usecase;

import com.pragma.powerup.domain.exception.InvalidPaginationException;

final class PaginationValidator {
    private static final int MIN_PAGE = 0;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 50;
    private static final String NEGATIVE_PAGE_MESSAGE = "The page number cannot be negative";
    private static final String INVALID_SIZE_MESSAGE = "The page size must be between 1 and 50";

    private PaginationValidator() {
    }

    static void validate(int page, int size) {
        if (page < MIN_PAGE) {
            throw new InvalidPaginationException(NEGATIVE_PAGE_MESSAGE);
        }
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new InvalidPaginationException(INVALID_SIZE_MESSAGE);
        }
    }
}
