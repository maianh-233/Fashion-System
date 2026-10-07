package com.fashionsystem.fashion_system.security;

import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerAccount;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.repository.CustomerAccountRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.RevokedTokenRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import com.fashionsystem.fashion_system.service.JwtService;
import com.fashionsystem.fashion_system.service.RbacService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

/** Validates access tokens and builds the same trusted principal for HTTP and STOMP. */
@Service
@RequiredArgsConstructor
public class AccessTokenAuthenticationService {

    private final JwtService jwtService;
    private final CustomerRepository customerRepository;
    private final CustomerAccountRepository customerAccountRepository;
    private final UserRepository userRepository;
    private final RbacService rbacService;
    private final RevokedTokenRepository revokedTokenRepository;

    public Optional<Authentication> authenticate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            if (revokedTokenRepository.existsById(jwtService.extractTokenId(token))) {
                return Optional.empty();
            }
            if (JwtService.ACCOUNT_TYPE_CUSTOMER.equals(jwtService.extractAccountType(token))) {
                return authenticateCustomer(token);
            }
            if (!JwtService.ACCOUNT_TYPE_USER.equals(jwtService.extractAccountType(token))) {
                return Optional.empty();
            }
            return authenticateUser(token);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private Optional<Authentication> authenticateUser(String token) {
        UUID userId = jwtService.extractUserId(token);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getActive()) || Boolean.TRUE.equals(user.getLocked())
                || user.getDeletedAt() != null || !jwtService.isTokenValid(token, user)) {
            return Optional.empty();
        }
        return Optional.of(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(user.getId(), user.getUsername()),
                null,
                rbacService.loadAuthorities(user.getId())));
    }

    private Optional<Authentication> authenticateCustomer(String token) {
        UUID customerId = jwtService.extractCustomerId(token);
        Customer customer = customerRepository.findById(customerId).orElse(null);
        CustomerAccount account = customerAccountRepository.findByCustomerId(customerId).orElse(null);
        if (customer == null || !Boolean.TRUE.equals(customer.getActive())
                || account == null || !jwtService.isCustomerTokenValid(token, customer, account)) {
            return Optional.empty();
        }
        return Optional.of(new UsernamePasswordAuthenticationToken(
                new AuthenticatedCustomer(customer.getId(), account.getUsername()),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }
}
