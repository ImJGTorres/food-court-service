package com.pragma.powerup.infrastructure.out.feign.adapter;

import com.pragma.powerup.domain.exception.NotificationRejectedException;
import com.pragma.powerup.domain.exception.UserServiceUnavailableException;
import com.pragma.powerup.domain.spi.IUserClientPort;
import com.pragma.powerup.infrastructure.out.feign.client.IUserFeignClient;
import com.pragma.powerup.infrastructure.out.feign.dto.UserFeignResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserFeignAdapter implements IUserClientPort {
    private static final String OWNER_ROLE = "OWNER";
    private static final String EMPLOYEE_ROLE = "EMPLOYEE";
    private static final String USER_SERVICE_UNAVAILABLE_MESSAGE = "The users service is not available";
    private static final String CLIENT_PHONE_NOT_FOUND_MESSAGE = "The phone of the client could not be found";

    private final IUserFeignClient userFeignClient;

    @Override
    public boolean isOwner(Long userId) {
        return hasRole(userId, OWNER_ROLE);
    }

    @Override
    public boolean isEmployee(Long userId) {
        return hasRole(userId, EMPLOYEE_ROLE);
    }

    @Override
    public String getClientPhone(Long clientId) {
        try {
            return userFeignClient.getClientPhone(clientId).getPhone();
        } catch (FeignException.NotFound exception) {
            throw new NotificationRejectedException(CLIENT_PHONE_NOT_FOUND_MESSAGE);
        } catch (FeignException exception) {
            throw new UserServiceUnavailableException(USER_SERVICE_UNAVAILABLE_MESSAGE);
        }
    }

    private boolean hasRole(Long userId, String role) {
        try {
            UserFeignResponse user = userFeignClient.getUserById(userId);
            return role.equals(user.getRole());
        } catch (FeignException.NotFound exception) {
            return false;
        } catch (FeignException exception) {
            throw new UserServiceUnavailableException(USER_SERVICE_UNAVAILABLE_MESSAGE);
        }
    }
}
