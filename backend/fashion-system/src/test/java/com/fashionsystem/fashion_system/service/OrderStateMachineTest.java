package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.OrderActorType;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.OrderType;
import com.fashionsystem.fashion_system.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.fashionsystem.fashion_system.entity.OrderActorType.*;
import static com.fashionsystem.fashion_system.entity.OrderStatus.*;
import static org.assertj.core.api.Assertions.*;

class OrderStateMachineTest {
    final OrderStateMachine machine = new OrderStateMachine();

    @Test void offlineOrdersStartCompletedAndOnlineOrdersStartPending() {
        assertThat(machine.initialStatus(OrderType.OFFLINE)).isEqualTo(COMPLETED);
        assertThat(machine.initialStatus(OrderType.ONLINE)).isEqualTo(PENDING);
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING,CONFIRMED,USER", "PENDING,CANCELLED,USER", "PENDING,CANCELLED,CUSTOMER", "PENDING,CANCELLED,SYSTEM",
            "CONFIRMED,HANDED_TO_SHIPPER,USER", "CONFIRMED,CANCELLED,USER",
            "HANDED_TO_SHIPPER,SHIPPING,SHIPPER", "HANDED_TO_SHIPPER,CONFIRMED,SHIPPER",
            "HANDED_TO_SHIPPER,COMPLETED,SHIPPER", "SHIPPING,COMPLETED,SHIPPER",
            "SHIPPING,DELIVERY_FAILED,SHIPPER", "DELIVERY_FAILED,RETURNED,USER"})
    void allowsDocumentedTransitions(OrderStatus from, OrderStatus to, OrderActorType actor) {
        assertThat(machine.canTransition(from, to, actor)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            // Khách không được hủy sau khi nhân viên đã xác nhận.
            "CONFIRMED,CANCELLED,CUSTOMER", "CONFIRMED,CANCELLED,SYSTEM",
            // Từ khi giao cho vận chuyển trở đi không ai được hủy.
            "HANDED_TO_SHIPPER,CANCELLED,USER", "SHIPPING,CANCELLED,USER", "DELIVERY_FAILED,CANCELLED,USER",
            // Chỉ bên vận chuyển báo đang giao / giao xong.
            "HANDED_TO_SHIPPER,SHIPPING,USER", "SHIPPING,COMPLETED,USER",
            // Hoàn hàng do nhân viên xác nhận, không tự động.
            "DELIVERY_FAILED,RETURNED,SHIPPER", "DELIVERY_FAILED,RETURNED,SYSTEM"})
    void rejectsTransitionsForWrongActor(OrderStatus from, OrderStatus to, OrderActorType actor) {
        assertThat(machine.canTransition(from, to, actor)).isFalse();
    }

    @Test void terminalStatusesHaveNoNextStatus() {
        for (var status : new OrderStatus[]{COMPLETED, CANCELLED, RETURNED}) {
            assertThat(status.isTerminal()).isTrue();
            for (var actor : OrderActorType.values())
                assertThat(machine.nextStatuses(status, actor)).isEmpty();
        }
    }

    @Test void unknownTransitionIsInvalidStateAndWrongActorIsForbidden() {
        assertThatThrownBy(() -> machine.requireTransition(COMPLETED, PENDING, USER))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        assertThatThrownBy(() -> machine.requireTransition(SHIPPING, COMPLETED, USER))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
    }

    @Test void nextStatusesDriveAvailableButtons() {
        assertThat(machine.nextStatuses(PENDING, USER)).containsExactlyInAnyOrder(CONFIRMED, CANCELLED);
        assertThat(machine.nextStatuses(PENDING, CUSTOMER)).containsExactly(CANCELLED);
        assertThat(machine.nextStatuses(CONFIRMED, USER)).containsExactlyInAnyOrder(HANDED_TO_SHIPPER, CANCELLED);
    }
}
