package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.order.SupportChatDtos;
import com.fashionsystem.fashion_system.entity.SupportFaq;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.SupportFaqRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Câu trả lời chính sách của bot hỗ trợ; áp dụng cho toàn chuỗi nên chỉ nhân viên cấp chuỗi được sửa. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("SUPPORT")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportFaqService {
    private final SupportFaqRepository repository;
    private final UserScopeService userScopes;

    public List<SupportChatDtos.Faq> list() {
        return repository.findAllByOrderBySortOrderAsc().stream().map(SupportFaqService::view).toList();
    }

    @Transactional
    public SupportChatDtos.Faq update(UUID actor, UUID id, SupportChatDtos.FaqUpdate request) {
        if (!userScopes.resolve(actor).isGlobal())
            throw BusinessException.forbidden("Chỉ nhân viên cấp chuỗi được sửa câu trả lời của bot");
        SupportFaq faq = repository.findById(id).orElseThrow(() -> BusinessException.notFound("Câu trả lời không tồn tại"));
        faq.setTitle(request.title().trim());
        faq.setAnswer(request.answer().trim());
        faq.setKeywords(request.keywords() == null || request.keywords().isBlank() ? null : request.keywords().trim());
        faq.setActive(request.active());
        faq.setUpdatedBy(actor);
        faq.setUpdatedAt(LocalDateTime.now());
        return view(repository.save(faq));
    }

    private static SupportChatDtos.Faq view(SupportFaq faq) {
        return new SupportChatDtos.Faq(faq.getId(), faq.getTopic(), faq.getTitle(), faq.getAnswer(), faq.getKeywords(),
                faq.getSortOrder(), Boolean.TRUE.equals(faq.getActive()), faq.getUpdatedAt());
    }
}
