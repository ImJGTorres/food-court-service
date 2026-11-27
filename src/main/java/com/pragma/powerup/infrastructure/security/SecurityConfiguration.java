package com.pragma.powerup.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfiguration {
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String OWNER_ROLE = "OWNER";
    private static final String CLIENT_ROLE = "CLIENT";
    private static final String EMPLOYEE_ROLE = "EMPLOYEE";
    private static final String RESTAURANTS_URL = "/api/v1/restaurants";
    private static final String DISHES_URL = "/api/v1/dishes/**";
    private static final String RESTAURANT_DISHES_URL = "/api/v1/restaurants/*/dishes";
    private static final String ORDERS_URL = "/api/v1/orders";
    private static final String ASSIGN_ORDER_URL = "/api/v1/orders/*/assign";
    private static final String READY_ORDER_URL = "/api/v1/orders/*/ready";
    private static final String DELIVER_ORDER_URL = "/api/v1/orders/*/deliver";
    private static final String CANCEL_ORDER_URL = "/api/v1/orders/*/cancel";
    private static final String RESTAURANT_EMPLOYEES_URL = "/api/v1/restaurants/*/employees";
    private static final String ERROR_URL = "/error";
    private static final String[] SWAGGER_URLS = {"/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"};

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenParser jwtTokenParser)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint())
                        .accessDeniedHandler(new JsonAccessDeniedHandler()))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(SWAGGER_URLS).permitAll()
                        .requestMatchers(ERROR_URL).permitAll()
                        .requestMatchers(HttpMethod.POST, RESTAURANTS_URL).hasRole(ADMIN_ROLE)
                        .requestMatchers(HttpMethod.GET, RESTAURANTS_URL).hasRole(CLIENT_ROLE)
                        .requestMatchers(HttpMethod.GET, RESTAURANT_DISHES_URL).hasRole(CLIENT_ROLE)
                        .requestMatchers(HttpMethod.POST, DISHES_URL).hasRole(OWNER_ROLE)
                        .requestMatchers(HttpMethod.PATCH, DISHES_URL).hasRole(OWNER_ROLE)
                        .requestMatchers(HttpMethod.POST, ORDERS_URL).hasRole(CLIENT_ROLE)
                        .requestMatchers(HttpMethod.GET, ORDERS_URL).hasRole(EMPLOYEE_ROLE)
                        .requestMatchers(HttpMethod.PATCH, ASSIGN_ORDER_URL).hasRole(EMPLOYEE_ROLE)
                        .requestMatchers(HttpMethod.PATCH, READY_ORDER_URL).hasRole(EMPLOYEE_ROLE)
                        .requestMatchers(HttpMethod.PATCH, DELIVER_ORDER_URL).hasRole(EMPLOYEE_ROLE)
                        .requestMatchers(HttpMethod.PATCH, CANCEL_ORDER_URL).hasRole(CLIENT_ROLE)
                        .requestMatchers(HttpMethod.POST, RESTAURANT_EMPLOYEES_URL).hasRole(OWNER_ROLE)
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenParser),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
