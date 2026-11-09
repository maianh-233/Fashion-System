package com.fashionsystem.fashion_system.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Dữ liệu vào / ra của module quản lý công việc. */
public final class WorkTaskDtos {
    private WorkTaskDtos() {
    }

    public record Ref(UUID id, String name) {}

    public record Person(UUID id, String fullName, String username, String jobTitle, UUID storeId, String storeName) {}

    /**
     * @param assignees người mà tài khoản hiện tại được giao việc
     * @param people    nhân viên trong phạm vi, dùng cho bộ lọc người nhận / người giao
     */
    public record Lookups(boolean canAssign, boolean canViewScope, boolean global, UUID scopeStoreId, List<Ref> stores,
                          List<Person> assignees, List<Person> people) {}

    public record ChecklistInput(UUID id, @NotBlank @Size(max = 500) String content) {}

    public record CreateRequest(@NotEmpty @Size(max = 100) List<UUID> assigneeIds,
                                @NotBlank @Size(max = 200) String title,
                                @Size(max = 5000) String description,
                                String priority,
                                LocalDateTime dueAt,
                                @Size(max = 50) List<@Valid ChecklistInput> checklist) {}

    public record UpdateRequest(@NotBlank @Size(max = 200) String title,
                                @Size(max = 5000) String description,
                                String priority,
                                LocalDateTime dueAt,
                                @Size(max = 50) List<@Valid ChecklistInput> checklist) {}

    public record CompleteRequest(@Size(max = 1000) String note) {}

    public record ReturnRequest(@NotBlank @Size(max = 1000) String reason) {}

    public record CancelRequest(@Size(max = 1000) String reason) {}

    public record ToggleRequest(boolean done) {}

    public record CommentRequest(@NotBlank @Size(max = 2000) String content) {}

    public record Task(UUID id, String code, UUID batchId, String title, String description, String priority, String status,
                       LocalDateTime dueAt, boolean overdue, boolean late, UUID assignerId, String assignerName,
                       UUID assigneeId, String assigneeName, UUID storeId, String storeName, int checklistTotal,
                       int checklistDone, int commentCount, String completionNote, LocalDateTime completedAt,
                       String returnReason, int returnCount, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record ChecklistItem(UUID id, String content, boolean done, LocalDateTime doneAt) {}

    public record Comment(UUID id, UUID authorId, String authorName, String content, LocalDateTime createdAt) {}

    public record History(UUID id, String action, String fromStatus, String toStatus, String note, String actorName,
                          LocalDateTime createdAt) {}

    /** Thao tác tài khoản hiện tại được làm trên một công việc; giao diện dựa vào đây để hiện nút. */
    public record Actions(boolean view, boolean start, boolean complete, boolean toggle, boolean edit, boolean giveBack,
                          boolean cancel, boolean comment, boolean viewBatch) {}

    public record Detail(Task task, List<ChecklistItem> checklist, List<Comment> comments, List<History> history,
                         List<Task> batch, Actions actions) {}

    public record StatRow(UUID assigneeId, String assigneeName, String storeName, int total, int open, int done, int overdue,
                          int onTime, int late, Integer onTimeRate) {}

    public record Stats(StatRow totals, List<StatRow> rows) {}
}
