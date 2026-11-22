package com.pragma.powerup.infrastructure.out.feign.adapter;

import com.pragma.powerup.domain.exception.UserServiceUnavailableException;
import com.pragma.powerup.domain.spi.IUserClientPort;
import com.pragma.powerup.infrastructure.out.feign.client.IUserFeignClient;
import com.pragma.powerup.infrastructure.out.feign.dto.UserFeignResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserFeignAdapter implements IUserClientPort {
    private static final String OWNER_ROLE = "OWNER";
    private static final String USER_SERVICE_UNAVAILABLE_MESSAGE = "The users service is not available";

    private final IUserFeignClient userFeignClient;

    @Override
    public boolean isOwner(Long userId) {
        try {
            UserFeignResponse user = userFeignClient.getUserById(userId);
            return OWNER_ROLE.equals(user.getRole());
        } catch (FeignException.NotFound exception) {
            return false;
        } catch (FeignException exception) {
            throw new UserServiceUnavailableException(USER_SERVICE_UNAVAILABLE_MESSAGE);
        }
    }

    @Override
    public boolean isEmployee(Long userId) {
        throw new UnsupportedOperationException("Implemented in step F3");
    }
}
