package com.fashionsystem.fashion_system.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fashionsystem.fashion_system.config.CustomerRefreshTokenCookieService;
import com.fashionsystem.fashion_system.config.CustomerRefreshTokenProperties;
import com.fashionsystem.fashion_system.config.RefreshTokenCookieService;
import com.fashionsystem.fashion_system.config.RefreshTokenProperties;
import org.junit.jupiter.api.Test;

class AuthRefreshIsolationTest {
    @Test
    void customerAndInternalRefreshCookiesHaveDifferentNamespaces() {
        RefreshTokenProperties internal = new RefreshTokenProperties();
        CustomerRefreshTokenProperties customer = new CustomerRefreshTokenProperties();

        var internalCookie = new RefreshTokenCookieService(internal).create("internal");
        var customerCookie = new CustomerRefreshTokenCookieService(customer).create("customer");

        assertThat(customerCookie.getName()).isNotEqualTo(internalCookie.getName());
        assertThat(customerCookie.getPath()).isEqualTo("/api/auth/customer");
        assertThat(internalCookie.getPath()).isEqualTo("/api/auth");
    }
}

