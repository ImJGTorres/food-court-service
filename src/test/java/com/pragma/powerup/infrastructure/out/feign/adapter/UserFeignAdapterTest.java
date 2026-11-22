package com.pragma.powerup.infrastructure.out.feign.adapter;

import com.pragma.powerup.domain.exception.UserServiceUnavailableException;
import com.pragma.powerup.infrastructure.out.feign.client.IUserFeignClient;
import com.pragma.powerup.infrastructure.out.feign.dto.UserFeignResponse;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFeignAdapterTest {
    private static final Long USER_ID = 1L;

    @Mock
    private IUserFeignClient userFeignClient;

    @InjectMocks
    private UserFeignAdapter userFeignAdapter;

    private UserFeignResponse buildUserWithRole(String role) {
        UserFeignResponse user = new UserFeignResponse();
        user.setId(USER_ID);
        user.setRole(role);
        return user;
    }

    private Request buildRequest() {
        return Request.create(Request.HttpMethod.GET, "/api/v1/users/1",
                Collections.emptyMap(), null, StandardCharsets.UTF_8);
    }

    @Test
    void isOwnerWhenUserHasOwnerRoleReturnsTrue() {
        when(userFeignClient.getUserById(USER_ID)).thenReturn(buildUserWithRole("OWNER"));

        assertTrue(userFeignAdapter.isOwner(USER_ID));
    }

    @Test
    void isOwnerWhenUserHasAnotherRoleReturnsFalse() {
        when(userFeignClient.getUserById(USER_ID)).thenReturn(buildUserWithRole("CLIENT"));

        assertFalse(userFeignAdapter.isOwner(USER_ID));
    }

    @Test
    void isOwnerWhenUsersServiceRespondsNotFoundReturnsFalse() {
        when(userFeignClient.getUserById(USER_ID)).thenThrow(
                new FeignException.NotFound("not found", buildRequest(), null, Collections.emptyMap()));

        assertFalse(userFeignAdapter.isOwner(USER_ID));
    }

    @Test
    void isOwnerWhenUsersServiceFailsThrowsUserServiceUnavailableException() {
        when(userFeignClient.getUserById(USER_ID)).thenThrow(
                new FeignException.InternalServerError("error", buildRequest(), null, Collections.emptyMap()));

        UserServiceUnavailableException exception = assertThrows(
                UserServiceUnavailableException.class, () -> userFeignAdapter.isOwner(USER_ID));

        assertEquals("The users service is not available", exception.getMessage());
    }

    @Test
    void isEmployeeWhenUserHasEmployeeRoleReturnsTrue() {
        when(userFeignClient.getUserById(USER_ID)).thenReturn(buildUserWithRole("EMPLOYEE"));

        assertTrue(userFeignAdapter.isEmployee(USER_ID));
    }

    @Test
    void isEmployeeWhenUserHasAnotherRoleReturnsFalse() {
        when(userFeignClient.getUserById(USER_ID)).thenReturn(buildUserWithRole("CLIENT"));

        assertFalse(userFeignAdapter.isEmployee(USER_ID));
    }

    @Test
    void isEmployeeWhenUsersServiceRespondsNotFoundReturnsFalse() {
        when(userFeignClient.getUserById(USER_ID)).thenThrow(
                new FeignException.NotFound("not found", buildRequest(), null, Collections.emptyMap()));

        assertFalse(userFeignAdapter.isEmployee(USER_ID));
    }

    @Test
    void isEmployeeWhenUsersServiceFailsThrowsUserServiceUnavailableException() {
        when(userFeignClient.getUserById(USER_ID)).thenThrow(
                new FeignException.InternalServerError("error", buildRequest(), null, Collections.emptyMap()));

        assertThrows(UserServiceUnavailableException.class, () -> userFeignAdapter.isEmployee(USER_ID));
    }
}
