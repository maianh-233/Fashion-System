package com.fashionsystem.fashion_system.recruitment;

import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.ApplicationDetail;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.ApplicationRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.ApplicationSummary;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.CandidateDetail;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.CandidateRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.CandidateSummary;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Document;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.DuplicateMatch;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.EvaluationForm;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.EvaluationRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.EvaluationTask;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.EvaluatorAssignment;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.HireRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.HireResult;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Interview;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.InterviewRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.InterviewStatusChange;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.MoveStage;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Note;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Offer;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.OfferRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.OfferResponse;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Reason;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.SignedUrl;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.StageDecision;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.StatusChange;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.ComposeEmail;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.EmailLog;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.EmailTemplate;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.EmailTemplateUpdate;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.RenderedEmail;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.SendOffer;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Tuyển dụng – nghiệp vụ: ứng viên & tài liệu, hồ sơ ứng tuyển, phiếu đánh giá, lịch phỏng vấn, offer, tuyển dụng. */
@RestController
@RequestMapping("/api/admin/recruitment")
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)"
        + " and hasAnyAuthority('RECRUITMENT_VIEW', 'RECRUITMENT_ADMIN')")
public class RecruitmentApplicationController {
    private static final String MANAGE = "hasAnyAuthority('RECRUITMENT_MANAGE', 'RECRUITMENT_ADMIN')";
    private static final String EVALUATE = "hasAnyAuthority('RECRUITMENT_EVALUATE', 'RECRUITMENT_ADMIN')";

    private final RecruitmentAccess access;
    private final CandidateService candidates;
    private final ApplicationService applications;
    private final EvaluationService evaluations;
    private final InterviewService interviews;
    private final OfferService offers;
    private final RecruitmentMailer mailer;

    public RecruitmentApplicationController(RecruitmentAccess access, CandidateService candidates,
                                            ApplicationService applications, EvaluationService evaluations,
                                            InterviewService interviews, OfferService offers, RecruitmentMailer mailer) {
        this.mailer = mailer;
        this.access = access;
        this.candidates = candidates;
        this.applications = applications;
        this.evaluations = evaluations;
        this.interviews = interviews;
        this.offers = offers;
    }

    // ------------------------------------------------------------------ ứng viên

    @GetMapping("/candidates")
    public Page<CandidateSummary> candidates(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String source,
                                             @RequestParam(required = false) String skill,
                                             @RequestParam(required = false) String tag,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) UUID positionId,
                                             @RequestParam(required = false) UUID campaignId,
                                             @RequestParam(required = false) UUID storeId,
                                             @RequestParam(required = false) Boolean hasCv,
                                             @PageableDefault(size = 20) Pageable pageable) {
        return candidates.list(access.current(), new CandidateService.Filter(keyword, source, skill, tag, status, positionId,
                campaignId, storeId, hasCv), pageable);
    }

    @GetMapping("/candidates/duplicates")
    public List<DuplicateMatch> duplicates(@RequestParam(required = false) String email,
                                           @RequestParam(required = false) String phone,
                                           @RequestParam(required = false) UUID excludeId) {
        return candidates.duplicates(access.current(), email, phone, excludeId);
    }

    @GetMapping("/candidates/{id}")
    public CandidateDetail candidate(@PathVariable UUID id) {
        return candidates.detail(access.current(), id);
    }

    @PostMapping("/candidates")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public CandidateDetail createCandidate(@Valid @RequestBody CandidateRequest request) {
        return candidates.create(access.current(), request);
    }

    @PutMapping("/candidates/{id}")
    @PreAuthorize(MANAGE)
    public CandidateDetail updateCandidate(@PathVariable UUID id, @Valid @RequestBody CandidateRequest request) {
        return candidates.update(access.current(), id, request);
    }

    @DeleteMapping("/candidates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('RECRUITMENT_ADMIN')")
    public void deleteCandidate(@PathVariable UUID id) {
        candidates.delete(access.current(), id);
    }

    @PostMapping(value = "/candidates/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public Document upload(@PathVariable UUID id, @RequestPart("file") MultipartFile file,
                           @RequestParam(defaultValue = "CV") String docType, @RequestParam(required = false) String note,
                           @RequestParam(required = false) UUID applicationId) {
        return candidates.upload(access.current(), id, file, docType, note, applicationId);
    }

    /** Link đã ký có hạn để xem (inline) hoặc tải tài liệu. */
    @GetMapping("/candidates/{id}/documents/{documentId}/url")
    public SignedUrl documentUrl(@PathVariable UUID id, @PathVariable UUID documentId,
                                 @RequestParam(defaultValue = "inline") String disposition) {
        return candidates.documentUrl(access.current(), id, documentId, !"attachment".equalsIgnoreCase(disposition));
    }

    @DeleteMapping("/candidates/{id}/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    public void deleteDocument(@PathVariable UUID id, @PathVariable UUID documentId) {
        candidates.deleteDocument(access.current(), id, documentId);
    }

    // ------------------------------------------------------------------ hồ sơ ứng tuyển

    @GetMapping("/applications")
    public Page<ApplicationSummary> applications(@RequestParam(required = false) UUID campaignId,
                                                 @RequestParam(required = false) UUID positionId,
                                                 @RequestParam(required = false) UUID candidateId,
                                                 @RequestParam(required = false) UUID stageId,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String source,
                                                 @RequestParam(required = false) UUID storeId,
                                                 @RequestParam(defaultValue = "false") boolean assignedToMe,
                                                 @PageableDefault(size = 20) Pageable pageable) {
        return applications.list(access.current(), new ApplicationService.Filter(campaignId, positionId, candidateId, stageId,
                status, keyword, source, storeId, assignedToMe), pageable);
    }

    @GetMapping("/applications/my-tasks")
    public List<EvaluationTask> myTasks() {
        return applications.myTasks(access.current());
    }

    @GetMapping("/applications/{id}")
    public ApplicationDetail application(@PathVariable UUID id) {
        return applications.detail(access.current(), id);
    }

    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public ApplicationDetail createApplication(@Valid @RequestBody ApplicationRequest request) {
        return applications.create(access.current(), request);
    }

    @PostMapping("/applications/{id}/status")
    @PreAuthorize(MANAGE)
    public ApplicationDetail applicationStatus(@PathVariable UUID id, @Valid @RequestBody StatusChange request) {
        return applications.changeStatus(access.current(), id, request.status(), request.reason(),
                Boolean.TRUE.equals(request.notifyCandidate()));
    }

    @PostMapping("/applications/{id}/advance")
    @PreAuthorize(MANAGE)
    public ApplicationDetail advance(@PathVariable UUID id, @RequestBody(required = false) MoveStage request) {
        return applications.advance(access.current(), id, request == null ? null : request.note());
    }

    @PostMapping("/applications/{id}/move-stage")
    @PreAuthorize(MANAGE)
    public ApplicationDetail moveStage(@PathVariable UUID id, @Valid @RequestBody MoveStage request) {
        return applications.moveToStage(access.current(), id, request.stageId(), request.note());
    }

    @PostMapping("/applications/{id}/stages/{stageId}/decision")
    @PreAuthorize("hasAnyAuthority('RECRUITMENT_MANAGE', 'RECRUITMENT_APPROVE', 'RECRUITMENT_ADMIN')")
    public ApplicationDetail decideStage(@PathVariable UUID id, @PathVariable UUID stageId,
                                         @Valid @RequestBody StageDecision request) {
        return applications.decideStage(access.current(), id, stageId, request.decision(), request.note());
    }

    @PutMapping("/applications/{id}/stages/{stageId}/evaluators")
    @PreAuthorize(MANAGE)
    public ApplicationDetail assignEvaluators(@PathVariable UUID id, @PathVariable UUID stageId,
                                              @Valid @RequestBody EvaluatorAssignment request) {
        return applications.assignEvaluators(access.current(), id, stageId, request.userIds());
    }

    @PostMapping("/applications/{id}/notes")
    public ApplicationDetail addNote(@PathVariable UUID id, @Valid @RequestBody Note request) {
        return applications.addNote(access.current(), id, request.message());
    }

    // ------------------------------------------------------------------ phiếu đánh giá

    @GetMapping("/applications/{id}/stages/{stageId}/evaluation")
    public EvaluationForm evaluationForm(@PathVariable UUID id, @PathVariable UUID stageId) {
        return evaluations.form(access.current(), id, stageId);
    }

    @PutMapping("/applications/{id}/stages/{stageId}/evaluation")
    @PreAuthorize(EVALUATE)
    public EvaluationForm saveEvaluation(@PathVariable UUID id, @PathVariable UUID stageId,
                                         @Valid @RequestBody EvaluationRequest request) {
        return evaluations.save(access.current(), id, stageId, request);
    }

    // ------------------------------------------------------------------ phỏng vấn

    @GetMapping("/interviews")
    public Page<Interview> interviews(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(defaultValue = "false") boolean mine,
                                      @RequestParam(required = false) UUID positionId,
                                      @RequestParam(required = false) UUID storeId,
                                      @PageableDefault(size = 50) Pageable pageable) {
        return interviews.list(access.current(), new InterviewService.Filter(from, to, status, mine, positionId, storeId), pageable);
    }

    @PostMapping("/applications/{id}/interviews")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public Interview createInterview(@PathVariable UUID id, @Valid @RequestBody InterviewRequest request) {
        return interviews.create(access.current(), id, request);
    }

    @PutMapping("/interviews/{id}")
    @PreAuthorize(MANAGE)
    public Interview updateInterview(@PathVariable UUID id, @Valid @RequestBody InterviewRequest request) {
        return interviews.update(access.current(), id, request);
    }

    @PostMapping("/interviews/{id}/status")
    public Interview interviewStatus(@PathVariable UUID id, @Valid @RequestBody InterviewStatusChange request) {
        return interviews.changeStatus(access.current(), id, request.status(), request.resultNote(),
                !Boolean.FALSE.equals(request.notifyCandidate()));
    }

    // ------------------------------------------------------------------ offer & tuyển dụng

    @GetMapping("/offers")
    public Page<Offer> offers(@RequestParam(required = false) String status, @RequestParam(required = false) UUID storeId,
                              @PageableDefault(size = 20) Pageable pageable) {
        return offers.list(access.current(), status, storeId, pageable);
    }

    @PostMapping("/applications/{id}/offers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public Offer createOffer(@PathVariable UUID id, @Valid @RequestBody OfferRequest request) {
        return offers.create(access.current(), id, request);
    }

    @PutMapping("/offers/{id}")
    @PreAuthorize(MANAGE)
    public Offer updateOffer(@PathVariable UUID id, @Valid @RequestBody OfferRequest request) {
        return offers.update(access.current(), id, request);
    }

    @PostMapping("/offers/{id}/submit")
    @PreAuthorize(MANAGE)
    public Offer submitOffer(@PathVariable UUID id) {
        return offers.submit(access.current(), id);
    }

    @PostMapping("/offers/{id}/approve")
    @PreAuthorize("hasAnyAuthority('RECRUITMENT_APPROVE', 'RECRUITMENT_ADMIN')")
    public Offer approveOffer(@PathVariable UUID id) {
        return offers.approve(access.current(), id);
    }

    @PostMapping("/offers/{id}/reject-approval")
    @PreAuthorize("hasAnyAuthority('RECRUITMENT_APPROVE', 'RECRUITMENT_ADMIN')")
    public Offer rejectOfferApproval(@PathVariable UUID id, @Valid @RequestBody Reason request) {
        return offers.rejectApproval(access.current(), id, request.reason());
    }

    @PostMapping("/offers/{id}/send")
    @PreAuthorize(MANAGE)
    public Offer sendOffer(@PathVariable UUID id, @RequestBody(required = false) SendOffer request) {
        return offers.send(access.current(), id, request == null || !Boolean.FALSE.equals(request.sendEmail()));
    }

    @PostMapping("/offers/{id}/respond")
    @PreAuthorize(MANAGE)
    public Offer respondOffer(@PathVariable UUID id, @Valid @RequestBody OfferResponse request) {
        return offers.respond(access.current(), id, request.accepted(), request.reason());
    }

    @PostMapping("/offers/{id}/cancel")
    @PreAuthorize(MANAGE)
    public Offer cancelOffer(@PathVariable UUID id, @Valid @RequestBody Reason request) {
        return offers.cancel(access.current(), id, request.reason());
    }

    // ------------------------------------------------------------------ email ứng viên

    @GetMapping("/applications/{id}/emails")
    public List<EmailLog> emails(@PathVariable UUID id) {
        requireSupervisor(id);
        return mailer.logs(id);
    }

    @GetMapping("/applications/{id}/emails/preview")
    public RenderedEmail previewEmail(@PathVariable UUID id, @RequestParam String templateCode) {
        requireSupervisor(id);
        return mailer.preview(id, templateCode);
    }

    @PostMapping("/applications/{id}/emails")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public EmailLog sendEmail(@PathVariable UUID id, @Valid @RequestBody ComposeEmail request) {
        RecruitmentAccess.Actor actor = access.current();
        access.requireManageApplication(actor, id);
        return mailer.sendManual(id, request.templateCode(), request.subject(), request.body(), actor.userId());
    }

    @PostMapping("/applications/{id}/emails/{emailId}/retry")
    @PreAuthorize(MANAGE)
    public EmailLog retryEmail(@PathVariable UUID id, @PathVariable UUID emailId) {
        access.requireManageApplication(access.current(), id);
        return mailer.retry(id, emailId);
    }

    @GetMapping("/email-templates")
    @PreAuthorize(MANAGE)
    public List<EmailTemplate> emailTemplates() {
        return mailer.templates(access.current());
    }

    @PutMapping("/email-templates/{code}")
    @PreAuthorize("hasAuthority('RECRUITMENT_ADMIN')")
    public EmailTemplate updateEmailTemplate(@PathVariable String code, @Valid @RequestBody EmailTemplateUpdate request) {
        return mailer.updateTemplate(access.current(), code, request.subject(), request.body(), request.enabled());
    }

    /** Email chứa thông tin ứng viên: chỉ người quản lý / duyệt hồ sơ được xem. */
    private void requireSupervisor(UUID applicationId) {
        RecruitmentAccess.Actor actor = access.current();
        UUID positionId = access.positionOfApplication(applicationId);
        if (!access.canManagePosition(actor, positionId) && !access.canApprovePosition(actor, positionId))
            throw com.fashionsystem.fashion_system.exception.BusinessException.forbidden("Bạn không có quyền xem email của hồ sơ này");
    }

    /** Chuyển ứng viên đã nhận offer thành nhân viên (cần thêm quyền tạo nhân viên của module Nhân sự). */
    @PostMapping("/applications/{id}/hire")
    @PreAuthorize(MANAGE + " and hasAuthority('USER_CREATE')")
    public HireResult hire(@PathVariable UUID id, @Valid @RequestBody HireRequest request) {
        return offers.hire(access.current(), id, request);
    }
}
