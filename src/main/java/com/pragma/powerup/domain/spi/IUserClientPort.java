package com.pragma.powerup.domain.spi;

public interface IUserClientPort {
    boolean isOwner(Long userId);

    boolean isEmployee(Long userId);

    String getClientPhone(Long clientId);
}
