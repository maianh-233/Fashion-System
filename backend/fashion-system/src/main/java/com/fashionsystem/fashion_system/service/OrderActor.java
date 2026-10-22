package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.OrderActorType;
import java.util.Objects;
import java.util.UUID;

/** Người hoặc hệ thống thực hiện thao tác trên đơn; id là user id hoặc customer id tùy loại. */
public record OrderActor(OrderActorType type, UUID id) {
    public OrderActor {
        Objects.requireNonNull(type, "type must not be null");
        if ((type == OrderActorType.USER || type == OrderActorType.CUSTOMER) && id == null)
            throw new IllegalArgumentException(type + " actor requires an id");
    }

    public static OrderActor user(UUID userId) {
        return new OrderActor(OrderActorType.USER, userId);
    }

    public static OrderActor customer(UUID customerId) {
        return new OrderActor(OrderActorType.CUSTOMER, customerId);
    }

    public static OrderActor system() {
        return new OrderActor(OrderActorType.SYSTEM, null);
    }

    public static OrderActor shipper() {
        return new OrderActor(OrderActorType.SHIPPER, null);
    }
}
