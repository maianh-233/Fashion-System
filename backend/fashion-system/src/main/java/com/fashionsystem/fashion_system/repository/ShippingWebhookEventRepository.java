package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.ShippingWebhookEvent;
import java.util.UUID;

public interface ShippingWebhookEventRepository extends BaseRepository<ShippingWebhookEvent, UUID> {
    boolean existsByProviderAndEventId(String provider, String eventId);
}
