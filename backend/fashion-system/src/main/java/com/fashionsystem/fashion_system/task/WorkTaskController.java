package com.fashionsystem.fashion_system.task;

import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Mọi nhân viên đều vào được; quyền trên từng công việc và phạm vi cửa hàng kiểm tra trong service. */
@RestController
@RequestMapping("/api/admin/tasks")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class WorkTaskController {
    private final WorkTaskService tasks;

    @GetMapping("/lookups")
    public WorkTaskDtos.Lookups lookups(@AuthenticationPrincipal AuthenticatedUser user) {
        return tasks.lookups(user.userId());
    }

    @GetMapping
    public Page<WorkTaskDtos.Task> list(@AuthenticationPrincipal AuthenticatedUser user,
                                        @RequestParam(required = false) UUID storeId,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) Boolean overdue,
                                        @RequestParam(required = false) UUID assigneeId,
                                        @RequestParam(required = false) UUID assignerId,
                                        @RequestParam(required = false) String priority,
                                        @RequestParam(required = false) String keyword,
                                        @PageableDefault(size = 20) Pageable pageable) {
        return tasks.list(user.userId(), storeId, status, overdue, assigneeId, assignerId, priority, keyword, pageable);
    }

    @GetMapping("/mine")
    public Page<WorkTaskDtos.Task> mine(@AuthenticationPrincipal AuthenticatedUser user,
                                        @RequestParam(required = false) String view,
                                        @RequestParam(required = false) String state,
                                        @RequestParam(required = false) String priority,
                                        @PageableDefault(size = 30) Pageable pageable) {
        return tasks.mine(user.userId(), view, state, priority, pageable);
    }

    @GetMapping("/stats")
    public WorkTaskDtos.Stats stats(@AuthenticationPrincipal AuthenticatedUser user,
                                    @RequestParam(required = false) UUID storeId,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return tasks.stats(user.userId(), storeId, from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<WorkTaskDtos.Task> create(@AuthenticationPrincipal AuthenticatedUser user,
                                          @Valid @RequestBody WorkTaskDtos.CreateRequest request) {
        return tasks.create(user.userId(), request);
    }

    @GetMapping("/{id}")
    public WorkTaskDtos.Detail detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return tasks.detail(user.userId(), id);
    }

    @PutMapping("/{id}")
    public WorkTaskDtos.Detail update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                      @Valid @RequestBody WorkTaskDtos.UpdateRequest request) {
        return tasks.update(user.userId(), id, request);
    }

    @PostMapping("/{id}/start")
    public WorkTaskDtos.Detail start(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return tasks.start(user.userId(), id);
    }

    @PostMapping("/{id}/complete")
    public WorkTaskDtos.Detail complete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                        @Valid @RequestBody(required = false) WorkTaskDtos.CompleteRequest request) {
        return tasks.complete(user.userId(), id, request);
    }

    @PostMapping("/{id}/return")
    public WorkTaskDtos.Detail giveBack(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                        @Valid @RequestBody WorkTaskDtos.ReturnRequest request) {
        return tasks.giveBack(user.userId(), id, request);
    }

    @PostMapping("/{id}/cancel")
    public WorkTaskDtos.Detail cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                      @Valid @RequestBody(required = false) WorkTaskDtos.CancelRequest request) {
        return tasks.cancel(user.userId(), id, request);
    }

    @PutMapping("/{id}/checklist/{itemId}")
    public WorkTaskDtos.Detail toggleItem(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                          @PathVariable UUID itemId, @RequestBody WorkTaskDtos.ToggleRequest request) {
        return tasks.toggleItem(user.userId(), id, itemId, request.done());
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkTaskDtos.Detail addComment(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                          @Valid @RequestBody WorkTaskDtos.CommentRequest request) {
        return tasks.addComment(user.userId(), id, request);
    }
}
