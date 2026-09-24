package com.fashionsystem.fashion_system.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import com.fashionsystem.fashion_system.dto.AuditLogDto;
import com.fashionsystem.fashion_system.audit.*;
import com.fashionsystem.fashion_system.entity.AuditLog;
import com.fashionsystem.fashion_system.entity.AuditOutbox;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.AuditLogRepository;
import com.fashionsystem.fashion_system.repository.AuditOutboxRepository;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Helper dùng chung: lấy actor từ SecurityContext và ghi audit cùng transaction nghiệp vụ. */
@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;
    private final AuditOutboxRepository outboxRepository;

    @Transactional
    public void record(String action, String entityType, UUID entityId, Object oldData, Object newData) {
        Actor actor = currentActor();
        JsonNode oldJson = toJson(oldData);
        JsonNode newJson = toJson(newData);
        Map<String, Object> details = new LinkedHashMap<>();
        if (oldJson != null) details.put("oldData", oldJson);
        if (newJson != null) details.put("newData", newJson);
        details.put("changedFields", changedFields(oldJson, newJson));
        outboxRepository.save(AuditOutbox.pending(AuditEvent.builder()
                .action(normalizeAction(action)).actorUserId(actor.id()).username(actor.username())
                .entityId(entityId).entityType(entityType)
                .ipAddress(requestValue("X-Forwarded-For", "X-Real-IP")).userAgent(requestValue("User-Agent"))
                .metadata(AuditEventSanitizer.sanitize(details)).build(), objectMapper));
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> search(UUID actorUserId, String action, String entityType, UUID entityId,
            String username, LocalDate from, LocalDate to, Pageable pageable) {
        LocalDateTime fromAt = from == null ? null : from.atStartOfDay();
        LocalDateTime toAt = to == null ? null : to.plusDays(1).atStartOfDay();
        return repository.search(actorUserId, blankToNull(action), blankToNull(entityType), entityId,
                blankToNull(username), fromAt, toAt, pageable).map(this::toDto);
    }

    private Actor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw BusinessException.unauthorized("Không xác định được nhân viên thực hiện thao tác");
        }
        return new Actor(principal.userId(), principal.username());
    }

    private JsonNode toJson(Object value) {
        if (value == null) return null;
        return objectMapper.valueToTree(value);
    }

    private JsonNode changedFields(JsonNode oldData, JsonNode newData) {
        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        if (oldData == null && newData == null) return result;
        if (oldData == null) { newData.propertyNames().forEach(result::add); return result; }
        if (newData == null) { oldData.propertyNames().forEach(result::add); return result; }
        if (oldData.isObject() && newData.isObject()) {
            for (String field : newData.propertyNames()) {
                if (!oldData.has(field) || !oldData.get(field).equals(newData.get(field))) result.add(field);
            }
            oldData.propertyNames().forEach(field -> { if (!newData.has(field)) result.add(field); });
        } else if (!oldData.equals(newData)) {
            result.add("value");
        }
        return result;
    }

    private String requestValue(String... names) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) return null;
        HttpServletRequest request = attributes.getRequest();
        for (String name : names) {
            String value = request.getHeader(name);
            if (value != null && !value.isBlank()) return name.equals("X-Forwarded-For") ? value.split(",")[0].trim() : value;
        }
        return null;
    }

    private AuditLogDto toDto(AuditLog a) {
        return new AuditLogDto(a.getId(), a.getActorUserId(), a.getUsername(), a.getAction(), a.getEntityType(),
                a.getEntityId(), a.getOldData(), a.getNewData(), a.getChangedFields(), a.getIpAddress(),
                a.getUserAgent(), a.getCreatedAt());
    }
    private String normalizeAction(String action) {
        if (action == null || !action.matches("[A-Z][A-Z0-9_]{1,39}")) throw BusinessException.badRequest("Audit action không hợp lệ");
        return action;
    }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private record Actor(UUID id, String username) {}
}
