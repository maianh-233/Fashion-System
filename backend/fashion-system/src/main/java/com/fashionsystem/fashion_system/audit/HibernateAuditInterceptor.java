package com.fashionsystem.fashion_system.audit;

import com.fashionsystem.fashion_system.entity.AuditLog;
import com.fashionsystem.fashion_system.entity.AuditOutbox;
import com.fashionsystem.fashion_system.entity.AuthAuditLog;
import com.fashionsystem.fashion_system.entity.PasswordResetOtp;
import com.fashionsystem.fashion_system.entity.PaymentWebhookLog;
import com.fashionsystem.fashion_system.entity.RevokedToken;
import com.fashionsystem.fashion_system.entity.UserToken;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** Captures Hibernate entity state transitions within an open audit scope. */
public final class HibernateAuditInterceptor implements Interceptor {
    private final ObjectMapper mapper;

    public HibernateAuditInterceptor(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    public boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        AuditCaptureScope.current().filter(ignored -> included(entity)).ifPresent(collector ->
                collector.recordInsert(table(entity), rowId(id), values(state, propertyNames, types)));
        return false;
    }

    @Override
    public boolean onFlushDirty(Object entity, Object id, Object[] currentState, Object[] previousState,
                                String[] propertyNames, Type[] types) {
        AuditCaptureScope.current().filter(ignored -> included(entity)).ifPresent(collector ->
                collector.recordUpdate(table(entity), rowId(id), values(previousState, propertyNames, types),
                        values(currentState, propertyNames, types)));
        return false;
    }

    @Override
    public void onRemove(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        AuditCaptureScope.current().filter(ignored -> included(entity)).ifPresent(collector ->
                collector.recordDelete(table(entity), rowId(id), values(state, propertyNames, types)));
    }

    private ObjectNode values(Object[] state, String[] propertyNames, Type[] types) {
        ObjectNode result = mapper.createObjectNode();
        if (state == null || propertyNames == null) return result;
        for (int i = 0; i < Math.min(state.length, propertyNames.length); i++) {
            if (types != null && i < types.length && types[i] != null
                    && (types[i].isAssociationType() || types[i].isCollectionType())) continue;
            result.set(propertyNames[i], mapper.valueToTree(state[i]));
        }
        return result;
    }

    private String rowId(Object id) {
        if (id == null) return "null";
        if (id instanceof String || id instanceof Number || id instanceof UUID) return id.toString();
        return mapper.valueToTree(id).toString();
    }

    private static String table(Object entity) {
        for (Class<?> type = entity.getClass(); type != null; type = type.getSuperclass()) {
            Table annotation = type.getAnnotation(Table.class);
            if (annotation != null) return annotation.name();
        }
        return entity.getClass().getSimpleName();
    }

    private static boolean included(Object entity) {
        return !(entity instanceof AuditLog || entity instanceof AuthAuditLog || entity instanceof AuditOutbox
                || entity instanceof RevokedToken || entity instanceof UserToken
                || entity instanceof PasswordResetOtp || entity instanceof PaymentWebhookLog);
    }
}
