package com.pragma.powerup.infrastructure.out.feign.configuration;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FeignAuthInterceptorTest {
    private static final String AUTHORIZATION = "Authorization";

    private final FeignAuthInterceptor feignAuthInterceptor = new FeignAuthInterceptor();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void applyCopiesTheAuthorizationHeaderOfTheCurrentRequestToTheFeignCall() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AUTHORIZATION, "Bearer abc");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        RequestTemplate template = new RequestTemplate();

        feignAuthInterceptor.apply(template);

        assertEquals(List.of("Bearer abc"), List.copyOf(template.headers().get(AUTHORIZATION)));
    }

    @Test
    void applyWhenTheCurrentRequestHasNoAuthorizationHeaderAddsNothing() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        RequestTemplate template = new RequestTemplate();

        feignAuthInterceptor.apply(template);

        assertFalse(template.headers().containsKey(AUTHORIZATION));
    }

    @Test
    void applyOutsideAnHttpRequestAddsNothingAndDoesNotFail() {
        RequestTemplate template = new RequestTemplate();

        feignAuthInterceptor.apply(template);

        assertFalse(template.headers().containsKey(AUTHORIZATION));
    }
}
