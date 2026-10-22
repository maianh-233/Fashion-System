package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.order.SupportChatDtos;
import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import com.fashionsystem.fashion_system.service.SupportChatService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Hội thoại hỗ trợ đơn hàng phía khách (giao diện khách nối sau). Tin realtime qua kênh SUPPORT_CHAT. */
@RestController
@RequestMapping("/api/customers/me/support")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerSupportChatController {
    private final SupportChatService service;

    /** Mở hội thoại cho đơn; lần đầu bot chào kèm menu gợi ý. */
    @PostMapping("/orders/{orderId}")
    public SupportChatDtos.Conversation open(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                             @PathVariable UUID orderId) {
        return service.open(customer.customerId(), orderId);
    }

    @GetMapping("/rooms/{roomId}")
    public SupportChatDtos.Conversation conversation(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                                     @PathVariable UUID roomId) {
        return service.customerConversation(customer.customerId(), roomId);
    }

    /** Gửi tin: {@code optionCode} khi bấm nút gợi ý, hoặc {@code text} khi tự gõ. */
    @PostMapping("/rooms/{roomId}/messages")
    public SupportChatDtos.Conversation send(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                             @PathVariable UUID roomId,
                                             @Valid @RequestBody SupportChatDtos.CustomerSend request) {
        return service.customerSend(customer.customerId(), roomId, request);
    }
}
