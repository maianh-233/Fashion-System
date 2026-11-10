package com.fashionsystem.fashion_system.marketing;

import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chiến dịch marketing. Mã quyền kiểm tra ở đây; phạm vi Global, trạng thái và luật tự duyệt kiểm tra trong service
 * ({@link MarketingCampaignAccess}).
 */
@RestController
@RequestMapping("/api/admin/marketing-campaigns")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class MarketingCampaignController {
    private static final String READ = "hasAnyAuthority('MARKETING_CAMPAIGN_VIEW', 'MARKETING_CAMPAIGN_MANAGE', 'MARKETING_CAMPAIGN_APPROVE')";
    private static final String MANAGE = "hasAuthority('MARKETING_CAMPAIGN_MANAGE')";
    private static final String APPROVE = "hasAuthority('MARKETING_CAMPAIGN_APPROVE')";

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final MarketingCampaignService campaigns;
    private final MarketingDispatchService dispatches;
    private final MarketingReportService reports;

    // ------------------------------------------------------------------ báo cáo và lịch (chỉ đọc)

    @GetMapping("/{id}/performance")
    @PreAuthorize(READ)
    public MarketingReportDtos.Performance performance(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return reports.performance(user.userId(), id);
    }

    @GetMapping("/{id}/performance/export")
    @PreAuthorize(READ)
    public ResponseEntity<byte[]> exportPerformance(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        byte[] body = reports.exportPerformance(user.userId(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("bao-cao-chien-dich_" + id + ".xlsx").build().toString())
                .contentType(XLSX).body(body);
    }

    @GetMapping("/compare")
    @PreAuthorize(READ)
    public MarketingReportDtos.Comparison compare(@AuthenticationPrincipal AuthenticatedUser user, @RequestParam List<UUID> ids) {
        return reports.compare(user.userId(), ids);
    }

    @GetMapping("/calendar")
    @PreAuthorize(READ)
    public MarketingReportDtos.Calendar calendar(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.calendar(user.userId(), from, to);
    }

    // ------------------------------------------------------------------ đợt gửi (thao tác ghi xong trả lại chi tiết chiến dịch)

    @PostMapping("/audience-preview")
    @PreAuthorize(MANAGE)
    public MarketingCampaignDtos.Preview audiencePreview(@AuthenticationPrincipal AuthenticatedUser user,
                                                         @Valid @RequestBody MarketingCampaignDtos.PreviewRequest request) {
        return dispatches.preview(user.userId(), request);
    }

    @PostMapping("/{id}/dispatches")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public MarketingCampaignDtos.Detail createDispatch(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                                       @Valid @RequestBody MarketingCampaignDtos.DispatchRequest request) {
        dispatches.create(user.userId(), id, request);
        return campaigns.detail(user.userId(), id);
    }

    @PutMapping("/{id}/dispatches/{dispatchId}")
    @PreAuthorize(MANAGE)
    public MarketingCampaignDtos.Detail updateDispatch(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                                       @PathVariable UUID dispatchId,
                                                       @Valid @RequestBody MarketingCampaignDtos.DispatchRequest request) {
        dispatches.update(user.userId(), id, dispatchId, request);
        return campaigns.detail(user.userId(), id);
    }

    @PostMapping("/{id}/dispatches/{dispatchId}/approve")
    @PreAuthorize(APPROVE)
    public MarketingCampaignDtos.Detail approveDispatch(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                                        @PathVariable UUID dispatchId) {
        dispatches.approve(user.userId(), id, dispatchId);
        return campaigns.detail(user.userId(), id);
    }

    @PostMapping("/{id}/dispatches/{dispatchId}/reject")
    @PreAuthorize(APPROVE)
    public MarketingCampaignDtos.Detail rejectDispatch(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                                       @PathVariable UUID dispatchId,
                                                       @Valid @RequestBody MarketingCampaignDtos.ReasonRequest request) {
        dispatches.reject(user.userId(), id, dispatchId, request.reason());
        return campaigns.detail(user.userId(), id);
    }

    @PostMapping("/{id}/dispatches/{dispatchId}/cancel")
    @PreAuthorize(MANAGE)
    public MarketingCampaignDtos.Detail cancelDispatch(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                                       @PathVariable UUID dispatchId,
                                                       @Valid @RequestBody MarketingCampaignDtos.ReasonRequest request) {
        dispatches.cancel(user.userId(), id, dispatchId, request.reason());
        return campaigns.detail(user.userId(), id);
    }

    @PostMapping("/{id}/dispatches/{dispatchId}/retry-failed")
    @PreAuthorize(MANAGE)
    public MarketingCampaignDtos.Detail retryDispatch(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                                      @PathVariable UUID dispatchId) {
        dispatches.retryFailed(user.userId(), id, dispatchId);
        return campaigns.detail(user.userId(), id);
    }

    @GetMapping("/{id}/dispatches/{dispatchId}/recipients")
    @PreAuthorize(READ)
    public Page<MarketingCampaignDtos.Recipient> recipients(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                                            @PathVariable UUID dispatchId,
                                                            @RequestParam(required = false) String status,
                                                            @PageableDefault(size = 20) Pageable pageable) {
        return dispatches.recipients(user.userId(), id, dispatchId, status, pageable);
    }

    @GetMapping("/lookups")
    @PreAuthorize(READ)
    public MarketingCampaignDtos.Lookups lookups(@AuthenticationPrincipal AuthenticatedUser user) {
        return campaigns.lookups(user.userId());
    }

    @GetMapping
    @PreAuthorize(READ)
    public Page<MarketingCampaignDtos.Campaign> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String phase,
                                                     @RequestParam(required = false) String objective,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                     @RequestParam(required = false) String q,
                                                     @PageableDefault(size = 20) Pageable pageable) {
        return campaigns.list(user.userId(), status, phase, objective, from, to, q, pageable);
    }

    @GetMapping("/offer-candidates")
    @PreAuthorize(MANAGE + " and hasAuthority('PROMOTION_VIEW')")
    public Page<MarketingCampaignDtos.Candidate> candidates(@AuthenticationPrincipal AuthenticatedUser user,
                                                            @RequestParam String type,
                                                            @RequestParam(required = false) String q,
                                                            @PageableDefault(size = 10) Pageable pageable) {
        return campaigns.candidates(user.userId(), type, q, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ)
    public MarketingCampaignDtos.Detail detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return campaigns.detail(user.userId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public MarketingCampaignDtos.Detail create(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody MarketingCampaignDtos.CampaignRequest request) {
        return campaigns.create(user.userId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(MANAGE)
    public MarketingCampaignDtos.Detail update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                               @Valid @RequestBody MarketingCampaignDtos.CampaignRequest request) {
        return campaigns.update(user.userId(), id, request);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize(MANAGE)
    public MarketingCampaignDtos.Detail submit(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return campaigns.submit(user.userId(), id);
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize(MANAGE)
    public MarketingCampaignDtos.Detail withdraw(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return campaigns.withdraw(user.userId(), id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(APPROVE)
    public MarketingCampaignDtos.Detail approve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                                @Valid @RequestBody(required = false) MarketingCampaignDtos.NoteRequest request) {
        return campaigns.approve(user.userId(), id, request == null ? null : request.note());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize(APPROVE)
    public MarketingCampaignDtos.Detail reject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                               @Valid @RequestBody MarketingCampaignDtos.ReasonRequest request) {
        return campaigns.reject(user.userId(), id, request.reason());
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize(MANAGE)
    public MarketingCampaignDtos.Detail reopen(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                               @Valid @RequestBody MarketingCampaignDtos.ReasonRequest request) {
        return campaigns.reopen(user.userId(), id, request.reason());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(MANAGE)
    public MarketingCampaignDtos.Detail cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                               @Valid @RequestBody MarketingCampaignDtos.ReasonRequest request) {
        return campaigns.cancel(user.userId(), id, request.reason());
    }

    @PostMapping("/{id}/offers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE + " and hasAuthority('PROMOTION_VIEW')")
    public MarketingCampaignDtos.Detail link(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                             @Valid @RequestBody MarketingCampaignDtos.LinkRequest request) {
        return campaigns.link(user.userId(), id, request);
    }

    @DeleteMapping("/{id}/offers/{offerId}")
    @PreAuthorize(MANAGE)
    public MarketingCampaignDtos.Detail unlink(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                               @PathVariable UUID offerId) {
        return campaigns.unlink(user.userId(), id, offerId);
    }

    @PostMapping("/{id}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public MarketingCampaignDtos.Detail addExpense(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                                   @Valid @RequestBody MarketingCampaignDtos.ExpenseRequest request) {
        return campaigns.addExpense(user.userId(), id, request);
    }

    @PutMapping("/{id}/expenses/{expenseId}")
    @PreAuthorize(MANAGE)
    public MarketingCampaignDtos.Detail updateExpense(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                                      @PathVariable UUID expenseId,
                                                      @Valid @RequestBody MarketingCampaignDtos.ExpenseRequest request) {
        return campaigns.updateExpense(user.userId(), id, expenseId, request);
    }

    @DeleteMapping("/{id}/expenses/{expenseId}")
    @PreAuthorize(MANAGE)
    public MarketingCampaignDtos.Detail deleteExpense(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                                      @PathVariable UUID expenseId) {
        return campaigns.deleteExpense(user.userId(), id, expenseId);
    }
}
