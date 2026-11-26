package com.pragma.powerup.infrastructure.out.pin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecureRandomPinAdapterTest {

    @Mock
    private SecureRandom secureRandom;

    @Test
    void generatePinCompletesWithLeadingZerosSoThePinAlwaysHasSixDigits() {
        when(secureRandom.nextInt(1_000_000)).thenReturn(42);

        assertEquals("000042", new SecureRandomPinAdapter(secureRandom).generatePin());
    }

    @Test
    void generatePinAcceptsTheSmallestAndTheBiggestPossibleNumber() {
        when(secureRandom.nextInt(1_000_000)).thenReturn(0, 999_999);
        SecureRandomPinAdapter adapter = new SecureRandomPinAdapter(secureRandom);

        assertEquals("000000", adapter.generatePin());
        assertEquals("999999", adapter.generatePin());
    }

    @Test
    void generatePinWithTheRealRandomAlwaysReturnsSixDigits() {
        SecureRandomPinAdapter adapter = new SecureRandomPinAdapter();

        for (int attempt = 0; attempt < 200; attempt++) {
            assertTrue(adapter.generatePin().matches("\\d{6}"));
        }
    }
}
