package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.PaymentWebhookLogDto;
import com.fashionsystem.fashion_system.entity.PaymentWebhookLog;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.PaymentWebhookLogMapper;
import com.fashionsystem.fashion_system.repository.PaymentWebhookLogRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lưu và truy vấn webhook payment, không cho sửa hoặc xóa payload gốc. */
@Service
@com.fashionsystem.fashion_system.audit.AuditInfrastructure(reason = "Webhook delivery bookkeeping")
@RequiredArgsConstructor
public class PaymentWebhookLogService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "provider", "eventType", "processed", "processedAt", "createdAt");
    private final PaymentWebhookLogRepository repository;
    private final PaymentWebhookLogMapper mapper;

    /** Ghi payload webhook gốc ở trạng thái chưa xử lý. */
    @Transactional
    public PaymentWebhookLogDto record(String provider, String eventType, String payload) {
        if (provider == null || provider.isBlank()) throw BusinessException.badRequest("Provider webhook là bắt buộc");
        if (payload == null || payload.isBlank()) throw BusinessException.badRequest("Payload webhook là bắt buộc");
        PaymentWebhookLog log = PaymentWebhookLog.builder().provider(provider.trim().toUpperCase(Locale.ROOT))
                .eventType(eventType == null ? null : eventType.trim().toUpperCase(Locale.ROOT))
                .payload(payload).processed(Boolean.FALSE).createdAt(LocalDateTime.now()).build();
        return mapper.toDto(repository.save(log));
    }

    /** Lấy chi tiết webhook log. */
    @Transactional(readOnly = true)
    public PaymentWebhookLogDto getById(UUID id) {
        return mapper.toDto(repository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Webhook log không tồn tại")));
    }

    /** Lấy webhook log theo provider, event, trạng thái và phân trang. */
    @Transactional(readOnly = true)
    public Page<PaymentWebhookLogDto> getList(
            String provider, String eventType, Boolean processed, Pageable pageable) {
        validateSort(pageable);
        return repository.search(normalize(provider), normalize(eventType), processed, pageable).map(mapper::toDto);
    }

    /** Đánh dấu webhook đã được xử lý mà không thay đổi payload gốc. */
    @Transactional
    public PaymentWebhookLogDto markProcessed(UUID id) {
        PaymentWebhookLog log = repository.findByIdForUpdate(id)
                .orElseThrow(() -> BusinessException.notFound("Webhook log không tồn tại"));
        if (!Boolean.TRUE.equals(log.getProcessed())) {
            log.setProcessed(Boolean.TRUE); log.setProcessedAt(LocalDateTime.now());
        }
        return mapper.toDto(repository.save(log));
    }

    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp webhook không hợp lệ");
    }
    private String normalize(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
}
