package com.fashionsystem.fashion_system.security;

import java.io.Serializable;
import java.util.UUID;

/** Principal riêng của khách hàng; không thể bị nhầm với nhân viên. */
public record AuthenticatedCustomer(UUID customerId, String username) implements Serializable {
}
