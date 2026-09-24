package com.fashionsystem.fashion_system.audit;

import com.fashionsystem.fashion_system.entity.AuditOutbox;
import com.fashionsystem.fashion_system.repository.AuditOutboxRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class BusinessAuditAspect {
    private final EntityManager entityManager;
    private final AuditOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final BusinessAuditContextFactory contextFactory;

    public BusinessAuditAspect(EntityManager entityManager, AuditOutboxRepository outboxRepository,
                               ObjectMapper objectMapper, BusinessAuditContextFactory contextFactory) {
        this.entityManager = entityManager;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.contextFactory = contextFactory;
    }

    @Around("@within(com.fashionsystem.fashion_system.audit.BusinessAudit) || "
            + "@annotation(com.fashionsystem.fashion_system.audit.BusinessAudit)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Business audit requires an active transaction");
        }
        if (AuditCaptureScope.current().isPresent()) return joinPoint.proceed();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        var method = AopUtils.getMostSpecificMethod(signature.getMethod(), joinPoint.getTarget().getClass());
        BusinessAudit marker = AnnotatedElementUtils.findMergedAnnotation(method, BusinessAudit.class);
        if (marker == null) marker = AnnotatedElementUtils.findMergedAnnotation(joinPoint.getTarget().getClass(), BusinessAudit.class);
        if (marker == null) throw new IllegalStateException("Business audit marker not found");
        BusinessAuditContext context = contextFactory.forCurrentRequest(marker.value(), method.getName());
        AuditChangeCollector collector = new AuditChangeCollector();
        try (AuditCaptureScope ignored = AuditCaptureScope.open(collector)) {
            Object result = joinPoint.proceed();
            entityManager.flush();
            List<AuditChange> changes = collector.finish();
            if (!changes.isEmpty()) outboxRepository.save(AuditOutbox.pending(context.event(changes), objectMapper));
            return result;
        }
    }
}
