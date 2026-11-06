package com.fashionsystem.fashion_system.payroll;

import com.fashionsystem.fashion_system.config.OrderDeliveryExecutorConfig;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.PermissionRecipientRepository;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Thông báo trong app: bảng lương chờ duyệt (người có quyền duyệt), phiếu lương đã phát hành (nhân viên). Gửi sau commit. */
@Slf4j
@Component
public class PayrollNotifier {
    static final String TYPE = "PAYROLL";
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM/yyyy");

    private final NotificationPublisher publisher;
    private final PermissionRecipientRepository recipients;
    private final NamedParameterJdbcTemplate jdbc;
    private final TaskExecutor delivery;

    public PayrollNotifier(NotificationPublisher publisher, PermissionRecipientRepository recipients, NamedParameterJdbcTemplate jdbc,
                           @Qualifier(OrderDeliveryExecutorConfig.EXECUTOR) TaskExecutor delivery) {
        this.publisher = publisher;
        this.recipients = recipients;
        this.jdbc = jdbc;
        this.delivery = delivery;
    }

    void approvalRequested(UUID periodId, String code, UUID actor) {
        List<UUID> approvers = recipients.findGlobalEligibleUserIds("PAYROLL_APPROVE");
        send(approvers, actor, "Bảng lương chờ duyệt", "Kỳ lương " + code + " đã được gửi phê duyệt.", periodId,
                "/admin/payroll/periods/" + periodId);
    }

    void payslipsPublished(UUID periodId, LocalDate month, UUID actor) {
        List<UUID> employees = jdbc.queryForList("select user_id from payroll_records where period_id = :id",
                new MapSqlParameterSource("id", periodId), UUID.class);
        send(employees, actor, "Phiếu lương tháng " + month.format(MONTH), "Phiếu lương tháng " + month.format(MONTH)
                + " của bạn đã được phát hành.", periodId, "/admin/me/payslips");
    }

    private void send(Collection<UUID> userIds, UUID actor, String title, String message, UUID periodId, String link) {
        List<RealtimeIdentity> to = userIds.stream().filter(Objects::nonNull).filter(id -> !id.equals(actor)).distinct()
                .map(RealtimeIdentity::user).toList();
        if (to.isEmpty()) return;
        NotificationRequest request = new NotificationRequest(to, actor, TYPE, title, message, "PAYROLL_PERIOD", periodId, link, Map.of());
        Runnable run = () -> delivery.execute(() -> {
            try {
                publisher.publish(request);
            } catch (RuntimeException e) {
                log.warn("Payroll notification failed: {}", title, e);
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    run.run();
                }
            });
        } else {
            run.run();
        }
    }
}
