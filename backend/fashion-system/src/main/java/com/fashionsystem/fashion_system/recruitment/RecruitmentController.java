package com.fashionsystem.fashion_system.recruitment;

import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.ApplyTemplate;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.CampaignDetail;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.CampaignRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.CampaignSummary;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Dashboard;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Lookups;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.PipelineRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.PositionDetail;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.PositionRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.PositionSummary;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.SaveAsTemplate;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Stage;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.StatusChange;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Template;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.TemplateRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.UserRef;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Tuyển dụng – cấu hình: dashboard, chiến dịch, vị trí, quy trình / tiêu chí, mẫu quy trình. Quyền theo vai trò ở đây;
 * phạm vi cửa hàng và phân công kiểm tra trong service ({@link RecruitmentAccess}).
 */
@RestController
@RequestMapping("/api/admin/recruitment")
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)"
        + " and hasAnyAuthority('RECRUITMENT_VIEW', 'RECRUITMENT_ADMIN')")
public class RecruitmentController {
    private static final String MANAGE = "hasAnyAuthority('RECRUITMENT_MANAGE', 'RECRUITMENT_ADMIN')";

    private final RecruitmentAccess access;
    private final RecruitmentLookupService lookups;
    private final RecruitmentDashboardService dashboard;
    private final CampaignService campaigns;
    private final PipelineService pipelines;

    public RecruitmentController(RecruitmentAccess access, RecruitmentLookupService lookups,
                                 RecruitmentDashboardService dashboard, CampaignService campaigns, PipelineService pipelines) {
        this.access = access;
        this.lookups = lookups;
        this.dashboard = dashboard;
        this.campaigns = campaigns;
        this.pipelines = pipelines;
    }

    @GetMapping("/lookups")
    public Lookups lookups() {
        return lookups.lookups(access.current());
    }

    @GetMapping("/lookups/users")
    public List<UserRef> users(@RequestParam(required = false) UUID storeId, @RequestParam(required = false) String keyword) {
        return lookups.users(access.current(), storeId, keyword);
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard(@RequestParam(required = false) UUID storeId, @RequestParam(required = false) UUID campaignId,
                               @RequestParam(required = false) UUID positionId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboard.dashboard(access.current(), new RecruitmentDashboardService.Filter(storeId, campaignId, positionId, from, to));
    }

    // ------------------------------------------------------------------ chiến dịch

    @GetMapping("/campaigns")
    public Page<CampaignSummary> campaigns(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) UUID departmentId,
                                           @RequestParam(required = false) UUID storeId,
                                           @RequestParam(required = false) Boolean headOffice,
                                           @PageableDefault(size = 20) Pageable pageable) {
        return campaigns.list(access.current(), keyword, status, departmentId, storeId, headOffice, pageable);
    }

    @GetMapping("/campaigns/{id}")
    public CampaignDetail campaign(@PathVariable UUID id) {
        return campaigns.detail(access.current(), id);
    }

    @PostMapping("/campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public CampaignDetail createCampaign(@Valid @RequestBody CampaignRequest request) {
        return campaigns.create(access.current(), request);
    }

    @PutMapping("/campaigns/{id}")
    @PreAuthorize(MANAGE)
    public CampaignDetail updateCampaign(@PathVariable UUID id, @Valid @RequestBody CampaignRequest request) {
        return campaigns.update(access.current(), id, request);
    }

    @PostMapping("/campaigns/{id}/status")
    @PreAuthorize(MANAGE)
    public CampaignDetail campaignStatus(@PathVariable UUID id, @Valid @RequestBody StatusChange request) {
        return campaigns.changeStatus(access.current(), id, request.status());
    }

    @DeleteMapping("/campaigns/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    public void deleteCampaign(@PathVariable UUID id) {
        campaigns.delete(access.current(), id);
    }

    // ------------------------------------------------------------------ vị trí

    @GetMapping("/positions")
    public List<PositionSummary> positions(@RequestParam(required = false) UUID campaignId,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Boolean openOnly) {
        return campaigns.positions(access.current(), campaignId, status, keyword, openOnly);
    }

    @GetMapping("/positions/{id}")
    public PositionDetail position(@PathVariable UUID id) {
        return campaigns.position(access.current(), id);
    }

    @PostMapping("/campaigns/{campaignId}/positions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public PositionDetail createPosition(@PathVariable UUID campaignId, @Valid @RequestBody PositionRequest request) {
        return campaigns.createPosition(access.current(), campaignId, request);
    }

    @PutMapping("/positions/{id}")
    @PreAuthorize(MANAGE)
    public PositionDetail updatePosition(@PathVariable UUID id, @Valid @RequestBody PositionRequest request) {
        return campaigns.updatePosition(access.current(), id, request);
    }

    @PostMapping("/positions/{id}/status")
    @PreAuthorize(MANAGE)
    public PositionDetail positionStatus(@PathVariable UUID id, @Valid @RequestBody StatusChange request) {
        return campaigns.changePositionStatus(access.current(), id, request.status());
    }

    @DeleteMapping("/positions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    public void deletePosition(@PathVariable UUID id) {
        campaigns.deletePosition(access.current(), id);
    }

    // ------------------------------------------------------------------ quy trình

    @PutMapping("/positions/{id}/stages")
    @PreAuthorize(MANAGE)
    public List<Stage> savePipeline(@PathVariable UUID id, @Valid @RequestBody PipelineRequest request) {
        return pipelines.savePipeline(access.current(), id, request.stages());
    }

    @PostMapping("/positions/{id}/apply-template")
    @PreAuthorize(MANAGE)
    public List<Stage> applyTemplate(@PathVariable UUID id, @Valid @RequestBody ApplyTemplate request) {
        return pipelines.applyTemplate(access.current(), id, request.templateId());
    }

    @PostMapping("/positions/{id}/save-as-template")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public Template saveAsTemplate(@PathVariable UUID id, @Valid @RequestBody SaveAsTemplate request) {
        return pipelines.saveAsTemplate(access.current(), id, request.name(), request.description());
    }

    @GetMapping("/templates")
    public List<Template> templates() {
        return pipelines.templates(access.current());
    }

    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public Template createTemplate(@Valid @RequestBody TemplateRequest request) {
        return pipelines.createTemplate(access.current(), request);
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize(MANAGE)
    public Template updateTemplate(@PathVariable UUID id, @Valid @RequestBody TemplateRequest request) {
        return pipelines.updateTemplate(access.current(), id, request);
    }

    @DeleteMapping("/templates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    public void deleteTemplate(@PathVariable UUID id) {
        pipelines.deleteTemplate(access.current(), id);
    }
}
