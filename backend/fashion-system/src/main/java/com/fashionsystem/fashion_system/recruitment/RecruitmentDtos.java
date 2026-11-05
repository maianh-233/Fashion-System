package com.fashionsystem.fashion_system.recruitment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Request / response của module tuyển dụng. */
public final class RecruitmentDtos {
    private RecruitmentDtos() {
    }

    public static final Set<String> SOURCES = Set.of("WEBSITE", "REFERRAL", "LINKEDIN", "TOPCV", "VIETNAMWORKS",
            "FACEBOOK", "JOB_FAIR", "HEADHUNTER", "WALK_IN", "OTHER");

    // ------------------------------------------------------------------ dùng chung

    public record UserRef(UUID id, String fullName, String username) {
    }

    public record Option(String value, String label) {
    }

    public record Lookups(List<Option> departments, List<OrgPosition> orgPositions, List<UserRef> users,
                          List<Option> sources, List<Option> applicationStatuses, List<Option> stageStatuses,
                          List<Option> employmentTypes, List<RoleOption> roles, List<Option> stores,
                          Permissions permissions, Scope scope) {
    }

    /** Phạm vi của người đang đăng nhập: storeId null = cấp chuỗi. */
    public record Scope(UUID storeId, String storeName) {
    }

    public record OrgPosition(UUID id, String name, UUID departmentId) {
    }

    public record RoleOption(String code, String name) {
    }

    /** Quyền của người đang đăng nhập (để frontend ẩn / hiện thao tác; backend vẫn kiểm tra lại). */
    public record Permissions(boolean admin, boolean manage, boolean evaluate, boolean approve) {
    }

    public record StatusChange(@NotBlank String status, @Size(max = 1000) String reason,
                               /** Gửi email thông báo kết quả cho ứng viên (khi chuyển sang "Không đạt"). */
                               Boolean notifyCandidate) {
        public StatusChange(String status, String reason) {
            this(status, reason, null);
        }
    }

    public record Note(@NotBlank @Size(max = 1000) String message) {
    }

    // ------------------------------------------------------------------ chiến dịch

    public record MemberRequest(@NotNull UUID userId, @NotBlank String role) {
    }

    public record CampaignRequest(
            @NotBlank @Size(max = 200) String name,
            /** Cửa hàng tuyển; null = văn phòng / toàn chuỗi. Nhân viên cửa hàng luôn bị gán cửa hàng của mình. */
            UUID storeId,
            UUID departmentId,
            UUID ownerId,
            @NotNull LocalDate startDate,
            LocalDate endDate,
            String description,
            String status,
            List<@Valid MemberRequest> members) {
    }

    public record Member(UUID userId, String fullName, String username, String role) {
    }

    public record CampaignSummary(UUID id, String code, String name, UUID storeId, String storeName,
                                  UUID departmentId, String departmentName,
                                  UUID ownerId, String ownerName, LocalDate startDate, LocalDate endDate,
                                  String status, int positionCount, int openPositionCount, int headcount, int hired,
                                  int applications, int inProcess, LocalDateTime createdAt) {
    }

    public record CampaignDetail(CampaignSummary campaign, String description, List<Member> members,
                                 List<PositionSummary> positions, boolean canManage) {
    }

    // ------------------------------------------------------------------ vị trí

    public record PositionRequest(
            @NotBlank @Size(max = 200) String title,
            UUID departmentId,
            UUID orgPositionId,
            UUID hiringManagerId,
            @NotNull @Min(1) @Max(1000) Integer headcount,
            @Size(max = 30) String employmentType,
            @Size(max = 200) String workLocation,
            String jobDescription,
            String requirements,
            String benefits,
            List<@NotBlank @Size(max = 60) String> skills,
            @DecimalMin("0") BigDecimal experienceMin,
            @DecimalMin("0") BigDecimal experienceMax,
            @DecimalMin("0") BigDecimal salaryMin,
            @DecimalMin("0") BigDecimal salaryMax,
            Boolean salaryNegotiable,
            LocalDate deadline,
            String status,
            Boolean isPublic,
            UUID templateId) {
    }

    public record PositionSummary(UUID id, String code, String title, UUID campaignId, String campaignName,
                                  String campaignStatus, UUID storeId, String storeName, UUID departmentId,
                                  String departmentName,
                                  UUID hiringManagerId, String hiringManagerName, int headcount, int hired,
                                  int applications, int inProcess, LocalDate deadline, String status,
                                  boolean isPublic, int stageCount) {
    }

    public record PositionDetail(PositionSummary position, UUID orgPositionId, String employmentType,
                                 String workLocation, String jobDescription, String requirements, String benefits,
                                 List<String> skills, BigDecimal experienceMin, BigDecimal experienceMax,
                                 BigDecimal salaryMin, BigDecimal salaryMax, boolean salaryNegotiable,
                                 List<Stage> stages, List<StageStat> stageStats, boolean canManage) {
    }

    public record StageStat(UUID stageId, int total, int pending, int inProgress, int passed, int failed, int current) {
    }

    // ------------------------------------------------------------------ quy trình & tiêu chí

    public record CriterionRequest(
            UUID id,
            @NotBlank @Size(max = 150) String name,
            String description,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal maxScore,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal weight,
            @NotBlank String importance) {
    }

    public record StageRequest(
            UUID id,
            @NotBlank @Size(max = 150) String name,
            String description,
            @NotBlank String stageType,
            UUID ownerId,
            @NotBlank String scoringMode,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal scaleMax,
            @DecimalMin("0") BigDecimal passScore,
            @NotBlank String passRule,
            @Min(1) @Max(20) Integer minEvaluations,
            Boolean autoAdvance,
            List<@Valid CriterionRequest> criteria) {
    }

    public record PipelineRequest(@NotNull List<@Valid StageRequest> stages) {
    }

    public record Criterion(UUID id, String name, String description, BigDecimal maxScore, BigDecimal weight,
                            String importance, int sortOrder) {
    }

    public record Stage(UUID id, String name, String description, int sortOrder, String stageType, UUID ownerId,
                        String ownerName, String scoringMode, BigDecimal scaleMax, BigDecimal passScore,
                        String passRule, int minEvaluations, boolean autoAdvance, List<Criterion> criteria) {
    }

    public record TemplateRequest(@NotBlank @Size(max = 150) String name, String description,
                                  @NotNull List<@Valid StageRequest> stages) {
    }

    public record SaveAsTemplate(@NotBlank @Size(max = 150) String name, String description) {
    }

    public record ApplyTemplate(@NotNull UUID templateId) {
    }

    public record Template(UUID id, String name, String description, List<StageRequest> stages,
                           String createdByName, LocalDateTime createdAt, LocalDateTime updatedAt, boolean canEdit) {
    }

    // ------------------------------------------------------------------ ứng viên

    public record CandidateRequest(
            @NotBlank @Size(max = 200) String fullName,
            @Email @Size(max = 255) String email,
            @Size(max = 20) String phone,
            LocalDate dateOfBirth,
            @Size(max = 20) String gender,
            @Size(max = 500) String address,
            @Size(max = 200) String currentTitle,
            @Size(max = 200) String currentCompany,
            @DecimalMin("0") BigDecimal yearsExperience,
            List<@NotBlank @Size(max = 60) String> skills,
            @Size(max = 500) String education,
            String summary,
            String source,
            @Size(max = 200) String sourceDetail,
            List<@NotBlank @Size(max = 40) String> tags,
            @Size(max = 500) String linkedinUrl,
            @Size(max = 500) String portfolioUrl,
            String notes,
            /** Ứng tuyển luôn vào các vị trí này khi tạo. */
            List<UUID> positionIds,
            /** Bỏ qua cảnh báo trùng và vẫn tạo ứng viên mới. */
            Boolean ignoreDuplicates) {
    }

    public record DuplicateMatch(UUID id, String code, String fullName, String email, String phone,
                                 List<String> matchedOn, int applicationCount, LocalDateTime createdAt) {
    }

    public record CandidateSummary(UUID id, String code, String fullName, String email, String phone,
                                   String currentTitle, BigDecimal yearsExperience, List<String> skills,
                                   String source, List<String> tags, int applicationCount, String latestStatus,
                                   String latestPosition, boolean hasCv, int duplicateCount, String storeName,
                                   LocalDateTime createdAt) {
    }

    public record Document(UUID id, String docType, int version, boolean current, String fileName,
                           String contentType, long size, String checksum, String storageProvider, String note,
                           UUID applicationId, String uploadedByName, LocalDateTime uploadedAt) {
    }

    public record CandidateDetail(UUID id, String code, String fullName, String email, String phone,
                                  LocalDate dateOfBirth, String gender, String address, String currentTitle,
                                  String currentCompany, BigDecimal yearsExperience, List<String> skills,
                                  String education, String summary, String source, String sourceDetail,
                                  List<String> tags, String linkedinUrl, String portfolioUrl, String notes,
                                  UUID storeId, String storeName, String createdByName, LocalDateTime createdAt,
                                  LocalDateTime updatedAt,
                                  List<Document> documents, List<ApplicationSummary> applications,
                                  List<DuplicateMatch> duplicates, UUID employeeId, String employeeCode,
                                  boolean canManage) {
    }

    public record SignedUrl(String url, long expiresInSeconds) {
    }

    // ------------------------------------------------------------------ hồ sơ ứng tuyển

    public record ApplicationRequest(@NotNull UUID candidateId, @NotNull UUID positionId, String source,
                                     String coverNote, Boolean notifyCandidate) {
    }

    public record ApplicationSummary(UUID id, String code, UUID candidateId, String candidateCode,
                                     String candidateName, String candidateEmail, String candidatePhone,
                                     UUID positionId, String positionTitle, UUID campaignId, String campaignName,
                                     String status, String statusLabel, UUID currentStageId, String currentStageName,
                                     String currentStageStatus, int stageIndex, int stageCount, BigDecimal lastScore,
                                     String source, LocalDateTime appliedAt, LocalDateTime updatedAt) {
    }

    public record EvaluatorView(UUID userId, String fullName, String evaluationStatus, BigDecimal totalScore,
                                String recommendation, LocalDateTime submittedAt) {
    }

    public record StageProgress(Stage stage, String status, String statusLabel, BigDecimal score,
                                String decidedByName, LocalDateTime decidedAt, String decisionNote,
                                LocalDateTime startedAt, List<EvaluatorView> evaluators, String myEvaluationStatus,
                                boolean canEvaluate) {
    }

    public record HistoryEntry(UUID id, String kind, String stageName, String fromStatus, String toStatus,
                               String message, String reason, String actorName, LocalDateTime createdAt) {
    }

    public record ApplicationDetail(ApplicationSummary application, String coverNote, String closedReason,
                                    UUID hiredEmployeeId, LocalDateTime hiredAt, List<Option> allowedStatuses,
                                    List<StageProgress> stages, List<Interview> interviews, List<Offer> offers,
                                    List<HistoryEntry> history, List<Document> documents, List<EmailLog> emails,
                                    String hiredEmployeeCode, boolean canManage, boolean canApprove) {
    }

    public record StageDecision(@NotBlank String decision, @Size(max = 1000) String note) {
    }

    public record MoveStage(UUID stageId, @Size(max = 1000) String note) {
    }

    public record EvaluatorAssignment(@NotNull List<UUID> userIds) {
    }

    // ------------------------------------------------------------------ đánh giá

    public record ScoreInput(@NotNull UUID criterionId, @DecimalMin("0") BigDecimal score, Boolean passed,
                             @Size(max = 1000) String comment) {
    }

    public record EvaluationRequest(@NotNull List<@Valid ScoreInput> scores, String recommendation, String comment,
                                    Boolean submit) {
    }

    public record ScoreView(UUID criterionId, BigDecimal score, Boolean passed, String comment) {
    }

    public record Evaluation(UUID id, UUID evaluatorId, String evaluatorName, String status, BigDecimal totalScore,
                             String recommendation, String comment, boolean criticalFailed,
                             List<ScoreView> scores, LocalDateTime submittedAt, LocalDateTime updatedAt) {
    }

    public record EvaluationForm(ApplicationSummary application, Stage stage, String stageStatus,
                                 BigDecimal stageScore, Evaluation mine, List<Evaluation> others,
                                 boolean othersHidden, boolean canEvaluate, boolean locked) {
    }

    public record EvaluationTask(UUID applicationId, String applicationCode, String candidateName,
                                 String positionTitle, UUID stageId, String stageName, String evaluationStatus,
                                 LocalDateTime nextInterviewAt, LocalDateTime assignedAt) {
    }

    // ------------------------------------------------------------------ phỏng vấn

    public record InterviewRequest(
            UUID stageId,
            @NotBlank @Size(max = 200) String title,
            @NotNull LocalDateTime startsAt,
            @NotNull LocalDateTime endsAt,
            @NotBlank String mode,
            @Size(max = 300) String location,
            @Size(max = 500) String meetingUrl,
            String note,
            @NotEmpty List<UUID> participantIds,
            /** Gửi thư mời / thông báo đổi lịch cho ứng viên (mặc định có). */
            Boolean notifyCandidate) {
    }

    public record InterviewStatusChange(@NotBlank String status, String resultNote, Boolean notifyCandidate) {
    }

    public record Interview(UUID id, UUID applicationId, String applicationCode, String candidateName,
                            String positionTitle, UUID stageId, String stageName, String title,
                            LocalDateTime startsAt, LocalDateTime endsAt, String mode, String location,
                            String meetingUrl, String status, String note, String resultNote,
                            List<UserRef> participants, String createdByName) {
    }

    // ------------------------------------------------------------------ offer & tuyển dụng

    public record OfferRequest(
            @NotBlank @Size(max = 200) String jobTitle,
            @NotNull @DecimalMin("0") BigDecimal salary,
            @NotNull LocalDate startDate,
            @NotNull LocalDate responseDeadline,
            String note,
            Boolean submitForApproval) {
    }

    public record OfferResponse(@NotNull Boolean accepted, @Size(max = 1000) String reason) {
    }

    public record Reason(@Size(max = 1000) String reason) {
    }

    public record Offer(UUID id, UUID applicationId, String applicationCode, String candidateName,
                        String positionTitle, String jobTitle, BigDecimal salary, LocalDate startDate,
                        LocalDate responseDeadline, String note, String status, String approvedByName,
                        LocalDateTime approvedAt, LocalDateTime sentAt, LocalDateTime respondedAt,
                        String declineReason, String createdByName, LocalDateTime createdAt, boolean canApprove,
                        boolean canManage) {
    }

    public record HireRequest(
            @Size(max = 50) String username,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 20) String phone,
            @NotBlank String employmentType,
            UUID departmentId,
            UUID orgPositionId,
            @NotEmpty Set<@NotBlank String> roleCodes,
            UUID storeId,
            /** Gửi email chào mừng (mã nhân viên, tên đăng nhập, ngày bắt đầu; không gửi mật khẩu). */
            Boolean sendWelcomeEmail) {
    }

    public record HireResult(UUID employeeId, String employeeCode, String username, String temporaryPassword,
                             List<String> transferred, boolean welcomeEmailQueued) {
    }

    public record SendOffer(Boolean sendEmail) {
    }

    // ------------------------------------------------------------------ email ứng viên

    public record EmailLog(UUID id, String templateCode, String toEmail, String subject, String body, String status,
                           String error, String sentByName, LocalDateTime createdAt, LocalDateTime sentAt) {
    }

    public record EmailTemplate(String code, String name, String subject, String body, boolean enabled,
                                String updatedByName, LocalDateTime updatedAt, boolean canEdit,
                                List<String> placeholders) {
    }

    public record EmailTemplateUpdate(@NotBlank @Size(max = 300) String subject, @NotBlank @Size(max = 10000) String body,
                                      @NotNull Boolean enabled) {
    }

    public record RenderedEmail(String toEmail, String subject, String body) {
    }

    public record ComposeEmail(String templateCode, @NotBlank @Size(max = 300) String subject,
                               @NotBlank @Size(max = 10000) String body) {
    }

    // ------------------------------------------------------------------ dashboard

    public record Kpis(int openCampaigns, int totalCampaigns, int openPositions, int totalHeadcount,
                       int totalApplications, int pendingApplications, int inProcess, int passed, int failed,
                       int hired, int withdrawn, int offersSent, int offersAccepted, int upcomingInterviews) {
    }

    public record FunnelStep(String key, String label, int entered, int passed, Double conversionRate) {
    }

    public record SourceStat(String source, int applications, int passedScreening, int offers, int hired,
                             Double hireRate) {
    }

    public record ProgressRow(UUID id, String code, String name, String parentName, String status, int headcount,
                              int hired, int applications, int inProcess, LocalDate deadline, Double fillRate) {
    }

    public record StatusCount(String status, String label, int count) {
    }

    public record Dashboard(Kpis kpis, List<FunnelStep> funnel, List<SourceStat> sources,
                            List<StatusCount> statuses, List<ProgressRow> campaigns, List<ProgressRow> positions,
                            List<Interview> upcomingInterviews, List<EvaluationTask> myTasks) {
    }

    // ------------------------------------------------------------------ công khai

    public record PublicPosition(String code, String title, String campaignName, String storeName,
                                 String storeAddress, String departmentName,
                                 String employmentType, String workLocation, int headcount, List<String> skills,
                                 BigDecimal experienceMin, BigDecimal experienceMax, BigDecimal salaryMin,
                                 BigDecimal salaryMax, boolean salaryNegotiable, LocalDate deadline,
                                 String jobDescription, String requirements, String benefits) {
    }

    public record PublicApplyResult(String applicationCode, String message) {
    }
}
