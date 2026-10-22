package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.order.SupportChatDtos;
import com.fashionsystem.fashion_system.entity.SupportChatStatus;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.SupportChatService;
import com.fashionsystem.fashion_system.service.SupportFaqService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Màn hình hỗ trợ đơn hàng của nhân viên và nội dung trả lời của bot. */
@RestController
@RequestMapping("/api/admin/support")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class AdminSupportChatController {
    private final SupportChatService chats;
    private final SupportFaqService faqs;

    @GetMapping("/rooms")
    @PreAuthorize("hasAuthority('ORDER_SUPPORT_CHAT')")
    public Page<SupportChatDtos.Room> rooms(Authentication auth, @RequestParam(required = false) UUID storeId,
                                            @RequestParam(required = false) List<SupportChatStatus> status,
                                            @PageableDefault(size = 20) Pageable pageable) {
        return chats.list(userId(auth), storeId, status, pageable);
    }

    @GetMapping("/rooms/{roomId}")
    @PreAuthorize("hasAuthority('ORDER_SUPPORT_CHAT')")
    public SupportChatDtos.Conversation conversation(Authentication auth, @PathVariable UUID roomId) {
        return chats.staffConversation(userId(auth), roomId);
    }

    @PostMapping("/rooms/{roomId}/claim")
    @PreAuthorize("hasAuthority('ORDER_SUPPORT_CHAT')")
    public SupportChatDtos.Conversation claim(Authentication auth, @PathVariable UUID roomId) {
        return chats.claim(userId(auth), roomId);
    }

    @PostMapping("/rooms/{roomId}/messages")
    @PreAuthorize("hasAuthority('ORDER_SUPPORT_CHAT')")
    public SupportChatDtos.Conversation reply(Authentication auth, @PathVariable UUID roomId,
                                              @Valid @RequestBody SupportChatDtos.StaffReply request) {
        return chats.reply(userId(auth), roomId, request.text());
    }

    @PostMapping("/rooms/{roomId}/close")
    @PreAuthorize("hasAuthority('ORDER_SUPPORT_CHAT')")
    public SupportChatDtos.Conversation close(Authentication auth, @PathVariable UUID roomId) {
        return chats.close(userId(auth), roomId);
    }

    @GetMapping("/faqs")
    @PreAuthorize("hasAuthority('ORDER_SUPPORT_CHAT') or hasAuthority('SUPPORT_FAQ_MANAGE')")
    public List<SupportChatDtos.Faq> faqs() {
        return faqs.list();
    }

    @PutMapping("/faqs/{id}")
    @PreAuthorize("hasAuthority('SUPPORT_FAQ_MANAGE')")
    public SupportChatDtos.Faq updateFaq(Authentication auth, @PathVariable UUID id,
                                         @Valid @RequestBody SupportChatDtos.FaqUpdate request) {
        return faqs.update(userId(auth), id, request);
    }

    private UUID userId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
