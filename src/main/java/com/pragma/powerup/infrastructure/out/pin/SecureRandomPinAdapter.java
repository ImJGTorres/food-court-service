package com.pragma.powerup.infrastructure.out.pin;

import com.pragma.powerup.domain.spi.IPinGeneratorPort;

import java.security.SecureRandom;

public class SecureRandomPinAdapter implements IPinGeneratorPort {
    private static final int PIN_LIMIT = 1_000_000;
    private static final String PIN_FORMAT = "%06d";

    private final SecureRandom secureRandom;

    public SecureRandomPinAdapter() {
        this(new SecureRandom());
    }

    public SecureRandomPinAdapter(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    @Override
    public String generatePin() {
        return String.format(PIN_FORMAT, secureRandom.nextInt(PIN_LIMIT));
    }
}
