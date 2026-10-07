package com.fashionsystem.fashion_system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.auth.CustomerAuthResponse;
import com.fashionsystem.fashion_system.dto.auth.CustomerInfoResponse;
import com.fashionsystem.fashion_system.dto.auth.LoginRequest;
import com.fashionsystem.fashion_system.dto.auth.RegisterCustomerRequest;
import com.fashionsystem.fashion_system.dto.auth.SocialLoginRequest;
import com.fashionsystem.fashion_system.entity.SocialProvider;
import com.fashionsystem.fashion_system.service.CustomerAuthService;
import com.fashionsystem.fashion_system.service.CustomerRefreshTokenService;
import com.fashionsystem.fashion_system.config.CustomerRefreshTokenCookieService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CustomerAuthControllerTest {
    @Test
    void customerEndpointsDelegateOnlyToCustomerAuthService() {
        CustomerAuthService service = mock(CustomerAuthService.class);
        CustomerRefreshTokenService refreshTokens = mock(CustomerRefreshTokenService.class);
        CustomerRefreshTokenCookieService cookies = mock(CustomerRefreshTokenCookieService.class);
        CustomerAuthController controller = new CustomerAuthController(service, refreshTokens, cookies);
        CustomerAuthResponse response = new CustomerAuthResponse(
                "token", "Bearer", 1000, new CustomerInfoResponse(
                        UUID.randomUUID(), "customer", "customer@example.com", "Customer"));
        RegisterCustomerRequest register = new RegisterCustomerRequest(
                "customer", "customer@example.com", "0901234567", "Password1!", "Customer");
        LoginRequest login = new LoginRequest("customer", "Password1!");
        SocialLoginRequest social = new SocialLoginRequest(SocialProvider.GOOGLE, "id-token");
        when(service.register(register)).thenReturn(response);
        when(service.login(login)).thenReturn(response);
        when(service.loginSocial(social)).thenReturn(response);
        when(refreshTokens.issueForCustomer(response.customer().id()))
                .thenReturn(new CustomerRefreshTokenService.IssuedToken("refresh"));
        when(cookies.create("refresh")).thenReturn(org.springframework.http.ResponseCookie
                .from("lunaria_customer_refresh_token", "refresh").path("/api/auth/customer").build());

        var registered = controller.register(register);
        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registered.getHeaders().getFirst("Set-Cookie"))
                .contains("lunaria_customer_refresh_token");
        assertThat(controller.login(login).getBody()).isSameAs(response);
        assertThat(controller.loginSocial(social).getBody()).isSameAs(response);
        verify(service).register(register);
        verify(service).login(login);
        verify(service).loginSocial(social);
    }
}
