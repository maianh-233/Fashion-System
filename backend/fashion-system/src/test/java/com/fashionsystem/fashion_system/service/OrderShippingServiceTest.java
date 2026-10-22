package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.*;
import com.fashionsystem.fashion_system.shipping.ShippingException;
import com.fashionsystem.fashion_system.shipping.ShippingModels;
import com.fashionsystem.fashion_system.shipping.ShippingModels.WebhookEvent;
import com.fashionsystem.fashion_system.shipping.ShippingModels.WebhookType;
import com.fashionsystem.fashion_system.shipping.ShippingProvider;
import com.fashionsystem.fashion_system.shipping.ShippingProviderRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderShippingServiceTest {
    final OrderAccessService access = mock(OrderAccessService.class);
    final OrderRepository orders = mock(OrderRepository.class);
    final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    final OrderStatusService statuses = new OrderStatusService(orders, mock(OrderStatusHistoryRepository.class),
            new OrderStateMachine(), events);
    final ShippingProviderRegistry registry = mock(ShippingProviderRegistry.class);
    final ShippingProvider provider = mock(ShippingProvider.class);
    final OrderItemRepository items = mock(OrderItemRepository.class);
    final OrderAddressRepository addresses = mock(OrderAddressRepository.class);
    final StoreRepository stores = mock(StoreRepository.class);
    final ShipmentRepository shipments = mock(ShipmentRepository.class);
    final ShippingWebhookEventRepository webhookEvents = mock(ShippingWebhookEventRepository.class);
    final CodCollectionService cod = mock(CodCollectionService.class);
    final OrderShippingService service = new OrderShippingService(access, new OrderStateMachine(), statuses,
            mock(OrderAdminService.class), registry, orders, items, addresses, stores, shipments, webhookEvents, events,
            cod);

    final UUID actor = UUID.randomUUID(), orderId = UUID.randomUUID(), storeId = UUID.randomUUID();
    final List<Shipment> saved = new ArrayList<>();

    Order order(OrderStatus status) {
        Order order = Order.builder().id(orderId).orderCode("LB-1").storeId(storeId).orderType(OrderType.ONLINE)
                .status(status).handlerUserId(actor).build();
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        return order;
    }

    @BeforeEach void setUp() {
        when(orders.save(any())).thenAnswer(call -> call.getArgument(0));
        when(registry.current()).thenReturn(provider);
        when(provider.code()).thenReturn("LALAMOVE");
        when(shipments.save(any())).thenAnswer(call -> {
            Shipment s = call.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            saved.add(s);
            return s;
        });
        when(webhookEvents.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        when(stores.findById(storeId)).thenReturn(Optional.of(Store.builder().id(storeId).name("Lunaria Q1")
                .address("1 Lê Lợi").phone("02838221234").latitude(new BigDecimal("10.7731")).longitude(new BigDecimal("106.7030")).build()));
        when(addresses.findByOrderId(orderId)).thenReturn(Optional.of(OrderAddress.builder().orderId(orderId)
                .receiverName("An").receiverPhone("0900000999").addressLine("2 Nguyễn Huệ").province("TP.HCM")
                .latitude(new BigDecimal("10.7769")).longitude(new BigDecimal("106.7009")).build()));
        when(items.findAllByOrderIdOrderByCreatedAtAsc(orderId)).thenReturn(List.of(OrderItem.builder().quantity(2).build()));
    }

    void providerAccepts() {
        when(provider.quote(any())).thenReturn(new ShippingModels.Quote("Q1", BigDecimal.valueOf(32000), "VND",
                Instant.now(), "S1", "S2", "{}"));
        when(provider.createShipment(any(), any())).thenReturn(new ShippingModels.CreatedShipment("LM-1",
                "ASSIGNING_DRIVER", ShipmentStatus.ASSIGNING_DRIVER, "https://share/x", BigDecimal.valueOf(32000), "VND", "{}"));
    }

    @Test void handoverCreatesFirstAttemptAndMovesOrder() {
        Order order = order(OrderStatus.CONFIRMED);
        when(access.requireOrderForUpdate(actor, orderId, OrderPermissions.HANDOVER_SHIPPING)).thenReturn(order);
        providerAccepts();

        service.handover(actor, orderId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.HANDED_TO_SHIPPER);
        Shipment shipment = saved.getFirst();
        assertThat(shipment.getAttemptNo()).isEqualTo(1);
        assertThat(shipment.getProviderOrderId()).isEqualTo("LM-1");
        assertThat(shipment.getFee()).isEqualByComparingTo("32000");
        var request = ArgumentCaptor.forClass(ShippingModels.ShipmentRequest.class);
        verify(provider).quote(request.capture());
        assertThat(request.getValue().pickup().name()).isEqualTo("Lunaria Q1");
        assertThat(request.getValue().dropoff().address()).isEqualTo("2 Nguyễn Huệ, TP.HCM");
        assertThat(request.getValue().itemQuantity()).isEqualTo(2);
    }

    @Test void retryAfterFailedAttemptIsAttemptTwo() {
        Order order = order(OrderStatus.CONFIRMED);
        when(access.requireOrderForUpdate(actor, orderId, OrderPermissions.HANDOVER_SHIPPING)).thenReturn(order);
        when(shipments.findAllByOrderIdOrderByCreatedAtDesc(orderId)).thenReturn(List.of(
                Shipment.builder().id(UUID.randomUUID()).status(ShipmentStatus.FAILED).attemptNo(1).build()));
        providerAccepts();
        service.handover(actor, orderId);
        assertThat(saved.getFirst().getAttemptNo()).isEqualTo(2);
    }

    @Test void cannotHandOverWhileAnotherShipmentIsActiveOrBeforeConfirmation() {
        Order pending = order(OrderStatus.PENDING);
        when(access.requireOrderForUpdate(actor, orderId, OrderPermissions.HANDOVER_SHIPPING)).thenReturn(pending);
        assertThatThrownBy(() -> service.handover(actor, orderId)).isInstanceOf(BusinessException.class);

        Order confirmed = order(OrderStatus.CONFIRMED);
        when(access.requireOrderForUpdate(actor, orderId, OrderPermissions.HANDOVER_SHIPPING)).thenReturn(confirmed);
        when(shipments.findAllByOrderIdOrderByCreatedAtDesc(orderId)).thenReturn(List.of(
                Shipment.builder().status(ShipmentStatus.ASSIGNING_DRIVER).build()));
        assertThatThrownBy(() -> service.handover(actor, orderId)).hasMessageContaining("chưa kết thúc");
        verifyNoInteractions(provider);
    }

    @Test void providerErrorIsBadGatewayAndChangesNothing() {
        Order order = order(OrderStatus.CONFIRMED);
        when(access.requireOrderForUpdate(actor, orderId, OrderPermissions.HANDOVER_SHIPPING)).thenReturn(order);
        when(provider.quote(any())).thenThrow(new ShippingException("Lalamove từ chối yêu cầu (422): ERR_INVALID_FIELD"));
        assertThatThrownBy(() -> service.handover(actor, orderId))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(502));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(saved).isEmpty();
    }

    @Test void providerShipmentIsCancelledWhenSavingFailsAfterwards() {
        Order order = order(OrderStatus.CONFIRMED);
        when(access.requireOrderForUpdate(actor, orderId, OrderPermissions.HANDOVER_SHIPPING)).thenReturn(order);
        providerAccepts();
        doThrow(new IllegalStateException("db down")).when(shipments).save(any());
        assertThatThrownBy(() -> service.handover(actor, orderId)).isInstanceOf(IllegalStateException.class);
        verify(provider).cancelShipment("LM-1");
    }

    // ---------- webhook ----------

    Shipment activeShipment(ShipmentStatus status) {
        Shipment shipment = Shipment.builder().id(UUID.randomUUID()).orderId(orderId).attemptNo(1)
                .shippingProvider("LALAMOVE").providerOrderId("LM-1").status(status).build();
        when(shipments.findByProviderOrderForUpdate("LALAMOVE", "LM-1")).thenReturn(Optional.of(shipment));
        return shipment;
    }

    WebhookEvent status(String providerStatus, ShipmentStatus mapped, String previous, String reason, Instant at) {
        return new WebhookEvent("E-" + UUID.randomUUID(), WebhookType.STATUS_CHANGED, "ORDER_STATUS_CHANGED", "LM-1",
                providerStatus, previous, mapped, reason, null, null, null, at, "{}");
    }

    @Test void duplicateEventIsNotAppliedTwice() {
        when(webhookEvents.existsByProviderAndEventId("LALAMOVE", "E-1")).thenReturn(true);
        var event = new WebhookEvent("E-1", WebhookType.STATUS_CHANGED, "ORDER_STATUS_CHANGED", "LM-1", "PICKED_UP",
                null, ShipmentStatus.PICKED_UP, null, null, null, null, Instant.now(), "{}");
        assertThat(service.applyWebhook("LALAMOVE", event)).isEqualTo(OrderShippingService.DUPLICATE);
        verifyNoInteractions(shipments);
    }

    @Test void pickupThenDeliveryCompletesTheOrder() {
        Order order = order(OrderStatus.HANDED_TO_SHIPPER);
        Shipment shipment = activeShipment(ShipmentStatus.DRIVER_ASSIGNED);
        service.applyWebhook("LALAMOVE", status("PICKED_UP", ShipmentStatus.PICKED_UP, "ON_GOING", null, Instant.parse("2026-10-14T03:00:00Z")));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPING);
        assertThat(shipment.getShippedAt()).isNotNull();
        service.applyWebhook("LALAMOVE", status("COMPLETED", ShipmentStatus.COMPLETED, "PICKED_UP", null, Instant.parse("2026-10-14T03:30:00Z")));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.COMPLETED);
        // Giao thành công thì ghi nhận tiền COD tài xế đã thu.
        verify(cod).recordCollected(order, "LM-1");
    }

    @Test void noDriverFoundReturnsOrderToConfirmedAndAlertsHandler() {
        Order order = order(OrderStatus.HANDED_TO_SHIPPER);
        Shipment shipment = activeShipment(ShipmentStatus.ASSIGNING_DRIVER);
        String result = service.applyWebhook("LALAMOVE", status("EXPIRED", ShipmentStatus.FAILED, "ASSIGNING_DRIVER",
                "Lalamove không tìm được tài xế", Instant.now()));
        assertThat(result).isEqualTo(OrderShippingService.APPLIED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.FAILED);
        assertThat(shipment.getFailureReason()).isEqualTo("Lalamove không tìm được tài xế");
        verify(events).publishEvent(argThat((Object e) -> e instanceof OrderEvents.ShipmentFailed failed
                && !failed.goodsWithDriver() && actor.equals(failed.handlerUserId())));
    }

    @Test void cancellationAfterPickupMeansDeliveryFailed() {
        Order order = order(OrderStatus.SHIPPING);
        activeShipment(ShipmentStatus.PICKED_UP);
        service.applyWebhook("LALAMOVE", status("CANCELED", ShipmentStatus.FAILED, "PICKED_UP", "Chuyến bị hủy", Instant.now()));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERY_FAILED);
        verify(events).publishEvent(argThat((Object e) -> e instanceof OrderEvents.ShipmentFailed failed && failed.goodsWithDriver()));
    }

    @Test void olderWebhookArrivingLateIsIgnored() {
        Order order = order(OrderStatus.SHIPPING);
        Shipment shipment = activeShipment(ShipmentStatus.PICKED_UP);
        shipment.setProviderUpdatedAt(LocalDateTime.of(2026, 10, 14, 11, 0));
        String result = service.applyWebhook("LALAMOVE", status("ON_GOING", ShipmentStatus.DRIVER_ASSIGNED, null, null,
                Instant.parse("2026-10-14T03:00:00Z"))); // 10:00 giờ VN, cũ hơn 11:00 đã ghi nhận
        assertThat(result).isEqualTo(OrderShippingService.STALE);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.PICKED_UP);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPING);
    }

    @Test void unknownShipmentAndUnknownStatusDoNotTouchData() {
        when(shipments.findByProviderOrderForUpdate(any(), any())).thenReturn(Optional.empty());
        assertThat(service.applyWebhook("LALAMOVE", status("PICKED_UP", ShipmentStatus.PICKED_UP, null, null, Instant.now())))
                .isEqualTo(OrderShippingService.UNKNOWN_SHIPMENT);
        Shipment shipment = activeShipment(ShipmentStatus.ASSIGNING_DRIVER);
        assertThat(service.applyWebhook("LALAMOVE", status("SOMETHING_NEW", null, null, null, Instant.now())))
                .isEqualTo(OrderShippingService.UNKNOWN_STATUS);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.ASSIGNING_DRIVER);
    }

    @Test void replacedShipmentResumesDeliveryOnNewProviderOrder() {
        Order order = order(OrderStatus.CONFIRMED);
        Shipment old = Shipment.builder().id(UUID.randomUUID()).orderId(orderId).attemptNo(1).shippingProvider("LALAMOVE")
                .providerOrderId("LM-1").status(ShipmentStatus.FAILED).build();
        when(shipments.findByProviderOrderForUpdate("LALAMOVE", "LM-1")).thenReturn(Optional.of(old));
        when(shipments.findAllByOrderIdOrderByCreatedAtDesc(orderId)).thenReturn(List.of(old));
        var event = new WebhookEvent("E-R", WebhookType.ORDER_REPLACED, "ORDER_REPLACED", "LM-2", null, null, null, null,
                "LM-1", null, null, Instant.now(), "{}");

        assertThat(service.applyWebhook("LALAMOVE", event)).isEqualTo(OrderShippingService.APPLIED);
        Shipment replacement = saved.getFirst();
        assertThat(replacement.getProviderOrderId()).isEqualTo("LM-2");
        assertThat(replacement.getAttemptNo()).isEqualTo(2);
        assertThat(old.getStatus()).isEqualTo(ShipmentStatus.REPLACED);
        assertThat(old.getReplacedByShipmentId()).isEqualTo(replacement.getId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.HANDED_TO_SHIPPER);
    }

    @Test void driverDetailsAreStored() {
        Shipment shipment = activeShipment(ShipmentStatus.ASSIGNING_DRIVER);
        var event = new WebhookEvent("E-D", WebhookType.DRIVER_ASSIGNED, "DRIVER_ASSIGNED", "LM-1", null, null, null, null,
                null, new ShippingModels.Driver("Tài xế A", "+84901234567", "59X1-12345"), null, Instant.now(), "{}");
        service.applyWebhook("LALAMOVE", event);
        assertThat(shipment.getDriverName()).isEqualTo("Tài xế A");
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DRIVER_ASSIGNED);
    }
}
