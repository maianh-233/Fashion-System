package com.fashionsystem.fashion_system.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.notification.NotificationType;
import com.fashionsystem.fashion_system.task.WorkTaskDtos.ChecklistInput;
import com.fashionsystem.fashion_system.task.WorkTaskDtos.CreateRequest;
import com.fashionsystem.fashion_system.task.WorkTaskDtos.Task;
import com.fashionsystem.fashion_system.task.WorkTaskDtos.UpdateRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** Chạy trên cơ sở dữ liệu thật với quyền và phạm vi thật; dữ liệu rollback sau mỗi test. */
@SpringBootTest
@Transactional
class WorkTaskIntegrationTest {
    @Autowired WorkTaskService tasks;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean WorkTaskNotifier notifier;

    final UUID storeA = UUID.randomUUID();
    final UUID storeB = UUID.randomUUID();
    final UUID admin = UUID.randomUUID();
    final UUID managerA = UUID.randomUUID();
    final UUID staffA = UUID.randomUUID();
    final UUID staffA2 = UUID.randomUUID();
    final UUID managerB = UUID.randomUUID();
    final UUID staffB = UUID.randomUUID();
    final UUID teamLead = UUID.randomUUID();
    final UUID report = UUID.randomUUID();
    String tag;

    @BeforeEach
    void setUp() {
        tag = storeA.toString().substring(0, 8);
        store(storeA, "A");
        store(storeB, "B");
        employee(admin, "admin", "ADMIN", null);
        employee(managerA, "man-a", "MANAGER", storeA);
        employee(staffA, "staff-a", "STAFF", storeA);
        employee(staffA2, "staff-a2", "STAFF", storeA);
        employee(managerB, "man-b", "MANAGER", storeB);
        employee(staffB, "staff-b", "STAFF", storeB);
        employee(teamLead, "lead", "STAFF", storeA);
        employee(report, "report", "STAFF", storeB);
        jdbc.update("update users set manager_id = ? where id = ?", teamLead, report);
    }

    @Test
    void assigningFollowsPermissionScopeAndDirectManager() {
        assertThat(tasks.lookups(managerA).assignees()).extracting(WorkTaskDtos.Person::id)
                .contains(staffA, staffA2, teamLead).doesNotContain(managerA, staffB, admin);
        assertThat(tasks.lookups(admin).assignees()).extracting(WorkTaskDtos.Person::id).contains(staffA, staffB, managerB);
        assertThat(tasks.lookups(staffA).canAssign()).isFalse();

        assertThat(create(managerA, List.of(staffA), null)).hasSize(1);
        assertThat(create(admin, List.of(staffB), null).getFirst().storeId()).isEqualTo(storeB);
        assertThatThrownBy(() -> create(managerA, List.of(staffB), null)).hasMessageContaining("ngoài phạm vi");
        assertThatThrownBy(() -> create(managerA, List.of(managerA), null)).hasMessageContaining("ngoài phạm vi");
        assertThatThrownBy(() -> create(staffA, List.of(staffA2), null)).hasMessageContaining("ngoài phạm vi");

        // Quản lý trực tiếp không có TASK_ASSIGN vẫn giao được cho cấp dưới, kể cả khác cửa hàng, nhưng không giao cho người khác.
        assertThat(create(teamLead, List.of(report), null)).hasSize(1);
        assertThatThrownBy(() -> create(teamLead, List.of(staffA), null)).hasMessageContaining("ngoài phạm vi");

        // Không giao ngược cho quản lý trực tiếp của mình.
        jdbc.update("update users set manager_id = ? where id = ?", staffA, managerA);
        assertThatThrownBy(() -> create(managerA, List.of(staffA), null)).hasMessageContaining("ngoài phạm vi");
    }

    @Test
    void multipleAssigneesGetSeparateTasksInOneBatch() {
        List<Task> created = create(managerA, List.of(staffA, staffA2), List.of("Bước 1"));
        assertThat(created).hasSize(2);
        assertThat(created.get(0).id()).isNotEqualTo(created.get(1).id());
        assertThat(created.get(0).batchId()).isEqualTo(created.get(1).batchId());
        assertThat(created.get(0).code()).startsWith("CV-");
        verify(notifier).send(eq(List.of(staffA)), eq(managerA), eq(NotificationType.TASK_ASSIGNED), anyString(), anyString(), any());
        verify(notifier).send(eq(List.of(staffA2)), eq(managerA), eq(NotificationType.TASK_ASSIGNED), anyString(), anyString(), any());

        UUID id = created.get(0).id();
        var detail = tasks.detail(staffA, id);
        job(() -> tasks.toggleItem(staffA, id, detail.checklist().getFirst().id(), true));
        job(() -> tasks.complete(staffA, id, null));

        assertThat(tasks.detail(managerA, id).batch()).extracting(Task::status).containsExactlyInAnyOrder("DONE", "TODO");
        assertThat(tasks.detail(staffA2, created.get(1).id()).batch()).isEmpty();
    }

    @Test
    void completionNotifiesAssignerAndAssignerCanGiveBack() {
        Task task = create(managerA, List.of(staffA), List.of("Bước 1", "Bước 2")).getFirst();
        UUID id = task.id();
        var items = tasks.detail(staffA, id).checklist();

        assertThatThrownBy(() -> job(() -> tasks.toggleItem(managerA, id, items.get(0).id(), true))).hasMessageContaining("người được giao");
        assertThat(job(() -> tasks.toggleItem(staffA, id, items.get(0).id(), true)).task().status()).isEqualTo("IN_PROGRESS");
        assertThatThrownBy(() -> job(() -> tasks.complete(staffA, id, null))).hasMessageContaining("Còn 1 việc con");
        verify(notifier, never()).send(any(), any(), eq(NotificationType.TASK_COMPLETED), any(), any(), any());

        job(() -> tasks.toggleItem(staffA, id, items.get(1).id(), true));
        var done = job(() -> tasks.complete(staffA, id, new WorkTaskDtos.CompleteRequest("Đã xong")));
        assertThat(done.task().status()).isEqualTo("DONE");
        assertThat(done.task().completedAt()).isNotNull();
        verify(notifier).send(eq(List.of(managerA)), eq(staffA), eq(NotificationType.TASK_COMPLETED), anyString(), eq("Đã xong"), any());

        assertThatThrownBy(() -> job(() -> tasks.giveBack(staffA, id, new WorkTaskDtos.ReturnRequest("x")))).hasMessageContaining("không có quyền");
        assertThatThrownBy(() -> job(() -> tasks.update(managerA, id, update("Tên mới", null, List.of())))).hasMessageContaining("đã kết thúc");

        var returned = job(() -> tasks.giveBack(managerA, id, new WorkTaskDtos.ReturnRequest("Thiếu ảnh")));
        assertThat(returned.task().status()).isEqualTo("IN_PROGRESS");
        assertThat(returned.task().returnCount()).isEqualTo(1);
        assertThat(returned.task().returnReason()).isEqualTo("Thiếu ảnh");
        assertThat(returned.task().completedAt()).isNull();
        verify(notifier).send(eq(List.of(staffA)), eq(managerA), eq(NotificationType.TASK_RETURNED), anyString(), anyString(), any());
        assertThat(returned.history()).extracting(WorkTaskDtos.History::action).contains("CREATED", "STARTED", "COMPLETED", "RETURNED");
    }

    @Test
    void accessIsLimitedToParticipantsAndScope() {
        UUID id = create(admin, List.of(staffA), null).getFirst().id();
        assertThat(tasks.detail(staffA, id).actions().complete()).isTrue();
        assertThat(tasks.detail(managerA, id).actions().cancel()).isTrue();       // TASK_ASSIGN cùng cửa hàng xử lý thay được
        assertThat(tasks.detail(managerA, id).actions().edit()).isFalse();        // nhưng chỉ người giao được sửa
        assertThatThrownBy(() -> tasks.detail(staffA2, id)).hasMessageContaining("không có quyền");
        assertThatThrownBy(() -> tasks.detail(managerB, id)).hasMessageContaining("không có quyền");
        assertThatThrownBy(() -> job(() -> tasks.complete(staffA2, id, null))).hasMessageContaining("người được giao");

        PageRequest page = PageRequest.of(0, 20);
        assertThat(tasks.list(managerA, null, null, null, null, null, null, null, page).getContent()).extracting(Task::id).contains(id);
        assertThat(tasks.list(managerB, null, null, null, null, null, null, null, page).getContent()).extracting(Task::id).doesNotContain(id);
        assertThatThrownBy(() -> tasks.list(managerB, storeA, null, null, null, null, null, null, page)).hasMessageContaining("không có quyền");
        assertThatThrownBy(() -> tasks.list(staffA, null, null, null, null, null, null, null, page)).hasMessageContaining("không có quyền");
        assertThat(tasks.mine(staffA, null, "OPEN", null, page).getContent()).extracting(Task::id).containsExactly(id);
        assertThat(tasks.mine(admin, "ASSIGNED_BY_ME", null, null, page).getContent()).extracting(Task::id).contains(id);

        job(() -> tasks.addComment(staffA, id, new WorkTaskDtos.CommentRequest("Đã nhận")));
        verify(notifier).send(any(), eq(staffA), eq(NotificationType.TASK_COMMENTED), anyString(), eq("Đã nhận"), any());
        assertThatThrownBy(() -> job(() -> tasks.addComment(staffA2, id, new WorkTaskDtos.CommentRequest("x")))).hasMessageContaining("không thể bình luận");

        var cancelled = job(() -> tasks.cancel(managerA, id, new WorkTaskDtos.CancelRequest("Không cần nữa")));
        assertThat(cancelled.task().status()).isEqualTo("CANCELLED");
        assertThatThrownBy(() -> job(() -> tasks.complete(staffA, id, null))).hasMessageContaining("đã kết thúc");
    }

    @Test
    void remindersAreSentOncePerDeadline() {
        UUID id = create(managerA, List.of(staffA), null, LocalDateTime.now().plusHours(5)).getFirst().id();
        clearInvocations(notifier);

        assertThat(tasks.sendDueReminders()).isGreaterThanOrEqualTo(1);
        verify(notifier, times(1)).send(eq(List.of(staffA)), eq(null), eq(NotificationType.TASK_DUE_SOON), anyString(), anyString(), any());
        clearInvocations(notifier);
        tasks.sendDueReminders();
        verify(notifier, never()).send(eq(List.of(staffA)), any(), eq(NotificationType.TASK_DUE_SOON), anyString(), anyString(), any());

        // Đổi hạn chót → được nhắc lại theo hạn mới.
        job(() -> tasks.update(managerA, id, update("Việc", LocalDateTime.now().plusHours(10), List.of())));
        tasks.sendDueReminders();
        verify(notifier).send(eq(List.of(staffA)), eq(null), eq(NotificationType.TASK_DUE_SOON), anyString(), anyString(), any());

        jdbc.update("update work_tasks set due_at = (now() at time zone 'UTC') - interval '1 hour' where id = ?", id);
        tasks.sendDueReminders();
        tasks.sendDueReminders();
        verify(notifier, times(1)).send(any(), eq(null), eq(NotificationType.TASK_OVERDUE), anyString(), anyString(), any());
        assertThat(tasks.detail(staffA, id).task().overdue()).isTrue();
        assertThat(tasks.list(managerA, null, null, true, null, null, null, null, PageRequest.of(0, 20)).getContent())
                .extracting(Task::id).contains(id);
        assertThatThrownBy(() -> create(managerA, List.of(staffA), null, LocalDateTime.now().minusHours(1))).hasMessageContaining("Hạn chót");
    }

    @Test
    void editingKeepsTickedChecklistItems() {
        UUID id = create(managerA, List.of(staffA), List.of("Giữ", "Xóa")).getFirst().id();
        var items = tasks.detail(staffA, id).checklist();
        job(() -> tasks.toggleItem(staffA, id, items.get(0).id(), true));
        clearInvocations(notifier);

        var edited = job(() -> tasks.update(managerA, id, update("Việc", null,
                List.of(new ChecklistInput(items.get(0).id(), "Giữ"), new ChecklistInput(null, "Mới")))));
        assertThat(edited.checklist()).extracting(WorkTaskDtos.ChecklistItem::content).containsExactly("Giữ", "Mới");
        assertThat(edited.checklist()).extracting(WorkTaskDtos.ChecklistItem::done).containsExactly(true, false);
        verify(notifier).send(eq(List.of(staffA)), eq(managerA), eq(NotificationType.TASK_UPDATED), anyString(), anyString(), any());

        clearInvocations(notifier);
        job(() -> tasks.update(managerA, id, update("Việc", null, edited.checklist().stream()
                .map(i -> new ChecklistInput(i.id(), i.content())).toList())));
        verify(notifier, never()).send(any(), any(), any(), any(), any(), any());      // không đổi gì thì không báo
        assertThatThrownBy(() -> job(() -> tasks.update(staffA, id, update("Việc", null, List.of())))).hasMessageContaining("người giao");
    }

    @Test
    void statsCountOnlyTasksOfTheScope() {
        UUID onTime = create(managerA, List.of(staffA), null, LocalDateTime.now().plusDays(2)).getFirst().id();
        UUID late = create(managerA, List.of(staffA), null, LocalDateTime.now().plusDays(2)).getFirst().id();
        UUID overdue = create(managerA, List.of(staffA2), null, LocalDateTime.now().plusDays(2)).getFirst().id();
        UUID cancelled = create(managerA, List.of(staffA2), null).getFirst().id();
        create(managerB, List.of(staffB), null);
        job(() -> tasks.complete(staffA, onTime, null));
        job(() -> tasks.complete(staffA, late, null));
        jdbc.update("update work_tasks set due_at = completed_at - interval '1 hour' where id = ?", late);
        jdbc.update("update work_tasks set due_at = (now() at time zone 'UTC') - interval '1 hour' where id = ?", overdue);
        job(() -> tasks.cancel(managerA, cancelled, null));

        WorkTaskDtos.Stats stats = tasks.stats(managerA, null, LocalDate.now().minusDays(1), LocalDate.now());
        assertThat(stats.totals().total()).isEqualTo(3);
        assertThat(stats.totals().done()).isEqualTo(2);
        assertThat(stats.totals().overdue()).isEqualTo(1);
        assertThat(stats.rows()).extracting(WorkTaskDtos.StatRow::assigneeId).containsExactly(staffA2, staffA);
        assertThat(stats.rows().get(1).onTimeRate()).isEqualTo(50);
        assertThat(stats.rows().get(0).onTimeRate()).isNull();
        assertThatThrownBy(() -> tasks.stats(staffA, null, LocalDate.now(), LocalDate.now())).hasMessageContaining("không có quyền");
    }

    private List<Task> create(UUID actor, List<UUID> assignees, List<String> checklist) {
        return create(actor, assignees, checklist, null);
    }

    private List<Task> create(UUID actor, List<UUID> assignees, List<String> checklist, LocalDateTime due) {
        List<ChecklistInput> items = checklist == null ? null : checklist.stream().map(c -> new ChecklistInput(null, c)).toList();
        return job(() -> tasks.create(actor, new CreateRequest(assignees, "Việc", null, null, due, items)));
    }

    private static UpdateRequest update(String title, LocalDateTime due, List<ChecklistInput> checklist) {
        return new UpdateRequest(title, null, null, due, checklist);
    }

    private void store(UUID id, String suffix) {
        jdbc.update("insert into stores(id, code, name, active) values (?, ?, ?, true)", id, "ZWT" + suffix + tag, "Zwt Store " + suffix);
    }

    private void employee(UUID id, String name, String role, UUID store) {
        jdbc.update("insert into users(id, username, full_name, active, locked) values (?, ?, ?, true, false)", id, "zwt-" + name + "-" + tag, "Zwt " + name);
        jdbc.update("insert into user_roles(user_id, role_id) select ?, id from roles where code = ?", id, role);
        if (store != null) jdbc.update("insert into store_staffs(user_id, store_id, active) values (?, ?, true)", id, store);
    }

    private <T> T job(Supplier<T> action) {
        try (var ignored = BusinessAuditContextFactory.systemJob("TEST_WORK_TASK")) {
            return action.get();
        }
    }
}
