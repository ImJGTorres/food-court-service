package com.pragma.powerup.infrastructure.out.pin;

import com.pragma.powerup.domain.spi.IPinGeneratorPort;

public class SecureRandomPinAdapter implements IPinGeneratorPort {

    @Override
    public String generatePin() {
        throw new UnsupportedOperationException("Implemented in step K2");
    }
}
