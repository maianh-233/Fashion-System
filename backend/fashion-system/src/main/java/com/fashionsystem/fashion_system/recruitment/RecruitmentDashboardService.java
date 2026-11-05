package com.fashionsystem.fashion_system.recruitment;

import com.fashionsystem.fashion_system.recruitment.RecruitmentAccess.Actor;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Dashboard;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.FunnelStep;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Kpis;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.ProgressRow;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.SourceStat;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.StatusCount;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dashboard tuyển dụng (chỉ tính dữ liệu người dùng được xem; nhân viên cửa hàng = cửa hàng của mình).
 *
 * <ul>
 *   <li>Đạt = hồ sơ đã qua toàn bộ quy trình (Đạt yêu cầu, Chờ / Đã gửi / Đã nhận / Từ chối offer, Đã tuyển).</li>
 *   <li>Không đạt = "Không đạt" + "Không đạt vòng tuyển dụng".</li>
 *   <li>Tỷ lệ chuyển đổi: theo loại vòng (vị trí khác nhau có số vòng khác nhau); chọn một vị trí thì theo từng vòng.</li>
 *   <li>Lọc thời gian theo ngày nộp hồ sơ.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class RecruitmentDashboardService {
    private final NamedParameterJdbcTemplate jdbc;
    private final InterviewService interviews;
    private final ApplicationService applications;

    public RecruitmentDashboardService(NamedParameterJdbcTemplate jdbc, InterviewService interviews,
                                       ApplicationService applications) {
        this.jdbc = jdbc;
        this.interviews = interviews;
        this.applications = applications;
    }

    public record Filter(UUID storeId, UUID campaignId, UUID positionId, LocalDate from, LocalDate to) {
    }

    private static final String APPS = """
             from job_applications a
             join recruitment_positions p on p.id = a.position_id
             join recruitment_campaigns c on c.id = p.campaign_id
            """;

    public Dashboard dashboard(Actor actor, Filter f) {
        MapSqlParameterSource params = RecruitmentAccess.params(actor);
        String appWhere = " where " + RecruitmentAccess.applicationVisible("a", "p", "c") + filters(f, params, true);
        String campaignWhere = " where " + RecruitmentAccess.campaignVisible("c") + filters(f, params, false);

        Kpis kpis = jdbc.queryForObject("""
                select
                  (select count(*) from recruitment_campaigns c""" + campaignWhere + """
                   and c.status = 'OPEN') as open_campaigns,
                  (select count(*) from recruitment_campaigns c""" + campaignWhere + """
                  ) as total_campaigns,
                  (select count(*) from recruitment_positions p join recruitment_campaigns c on c.id = p.campaign_id""" + campaignWhere + positionFilter(f) + """
                   and p.status = 'OPEN' and c.status = 'OPEN') as open_positions,
                  (select coalesce(sum(p.headcount), 0) from recruitment_positions p join recruitment_campaigns c on c.id = p.campaign_id""" + campaignWhere + positionFilter(f) + """
                   and p.status in ('OPEN', 'PAUSED', 'FILLED') and c.status in ('OPEN', 'PAUSED')) as headcount,
                  (select count(*)""" + APPS + appWhere + """
                  ) as total,
                  (select count(*)""" + APPS + appWhere + """
                   and a.status in ('NEW', 'SCREENING_PENDING')) as pending,
                  (select count(*)""" + APPS + appWhere + " and a.status in " + CampaignService.IN_PROCESS + """
                  ) as in_process,
                  (select count(*)""" + APPS + appWhere + """
                   and a.status in ('QUALIFIED', 'OFFER_PENDING', 'OFFER_SENT', 'OFFER_ACCEPTED', 'OFFER_DECLINED', 'HIRED')) as passed,
                  (select count(*)""" + APPS + appWhere + """
                   and a.status in ('REJECTED', 'ROUND_FAILED')) as failed,
                  (select count(*)""" + APPS + appWhere + """
                   and a.status = 'HIRED') as hired,
                  (select count(*)""" + APPS + appWhere + """
                   and a.status = 'WITHDRAWN') as withdrawn,
                  (select count(*) from recruitment_offers o join job_applications a on a.id = o.application_id
                     join recruitment_positions p on p.id = a.position_id join recruitment_campaigns c on c.id = p.campaign_id""" + appWhere + """
                   and o.status in ('SENT', 'ACCEPTED', 'DECLINED', 'EXPIRED')) as offers_sent,
                  (select count(*) from recruitment_offers o join job_applications a on a.id = o.application_id
                     join recruitment_positions p on p.id = a.position_id join recruitment_campaigns c on c.id = p.campaign_id""" + appWhere + """
                   and o.status = 'ACCEPTED') as offers_accepted,
                  (select count(*) from recruitment_interviews i join job_applications a on a.id = i.application_id
                     join recruitment_positions p on p.id = a.position_id join recruitment_campaigns c on c.id = p.campaign_id""" + appWhere + """
                   and i.status = 'SCHEDULED' and i.starts_at >= now() at time zone 'UTC'
                   and i.starts_at < (now() at time zone 'UTC') + interval '7 days') as upcoming
                """, params, (rs, i) -> new Kpis(rs.getInt("open_campaigns"), rs.getInt("total_campaigns"),
                rs.getInt("open_positions"), rs.getInt("headcount"), rs.getInt("total"), rs.getInt("pending"),
                rs.getInt("in_process"), rs.getInt("passed"), rs.getInt("failed"), rs.getInt("hired"), rs.getInt("withdrawn"),
                rs.getInt("offers_sent"), rs.getInt("offers_accepted"), rs.getInt("upcoming")));

        List<StatusCount> statuses = new ArrayList<>();
        jdbc.query("select a.status, count(*) as n" + APPS + appWhere + " group by a.status", params, rs -> {
            ApplicationStatus s = ApplicationStatus.valueOf(rs.getString("status"));
            statuses.add(new StatusCount(s.name(), s.label(), rs.getInt("n")));
        });
        statuses.sort((x, y) -> ApplicationStatus.valueOf(x.status()).ordinal() - ApplicationStatus.valueOf(y.status()).ordinal());

        return new Dashboard(kpis, funnel(f, params, appWhere, kpis), sources(params, appWhere),
                statuses, campaignProgress(params, campaignWhere), positionProgress(f, params, campaignWhere),
                interviews.list(actor, new InterviewService.Filter(LocalDateTime.now(), LocalDateTime.now().plusDays(7),
                        "SCHEDULED", false, f.positionId(), f.storeId()), Pageable.ofSize(8)).getContent(),
                applications.myTasks(actor));
    }

    /** Điều kiện lọc chung; {@code forApplications} = thêm lọc theo ngày nộp. */
    private static String filters(Filter f, MapSqlParameterSource params, boolean forApplications) {
        StringBuilder sql = new StringBuilder();
        if (f.storeId() != null) { sql.append(" and c.store_id = :fStore"); params.addValue("fStore", f.storeId()); }
        if (f.campaignId() != null) { sql.append(" and c.id = :fCampaign"); params.addValue("fCampaign", f.campaignId()); }
        if (forApplications) {
            if (f.positionId() != null) { sql.append(" and a.position_id = :fPosition"); params.addValue("fPosition", f.positionId()); }
            if (f.from() != null) { sql.append(" and a.applied_at >= :fFrom"); params.addValue("fFrom", DbTimes.toDb(f.from().atStartOfDay())); }
            if (f.to() != null) { sql.append(" and a.applied_at < :fTo"); params.addValue("fTo", DbTimes.toDb(f.to().plusDays(1).atStartOfDay())); }
        }
        // Khoảng trắng cuối: các text block nối phía sau bị cắt thụt lề đầu dòng.
        return sql.append(' ').toString();
    }

    private static String positionFilter(Filter f) {
        return f.positionId() == null ? " " : " and p.id = :fPosition ";
    }

    private List<FunnelStep> funnel(Filter f, MapSqlParameterSource params, String appWhere, Kpis kpis) {
        List<FunnelStep> steps = new ArrayList<>();
        steps.add(new FunnelStep("APPLIED", "Ứng tuyển", kpis.totalApplications(), kpis.totalApplications(), null));
        if (f.positionId() != null) {
            params.addValue("fPosition", f.positionId());
            jdbc.query("with va as (select a.id" + APPS + appWhere + """
                    )
                    select s.id, s.name, count(r.*) filter (where r.status <> 'SKIPPED') as entered,
                           count(r.*) filter (where r.status = 'PASSED') as passed
                    from recruitment_stages s
                    left join application_stage_results r on r.stage_id = s.id and r.application_id in (select id from va)
                    where s.position_id = :fPosition group by s.id, s.name, s.sort_order order by s.sort_order""", params, rs -> {
                steps.add(step(rs.getString("id"), rs.getString("name"), rs.getInt("entered"), rs.getInt("passed")));
            });
        } else {
            String[][] types = {{"SCREENING", "Sàng lọc"}, {"TEST", "Kiểm tra"}, {"INTERVIEW", "Phỏng vấn"}, {"OFFER", "Offer"}};
            for (String[] type : types) {
                params.addValue("fType", type[0]);
                // Mỗi hồ sơ tính một lần cho mỗi loại vòng: vào nếu có ít nhất một vòng loại đó, đạt nếu mọi vòng loại đó đã đi qua đều đạt.
                jdbc.query("""
                        select count(*) filter (where entered) as entered, count(*) filter (where entered and passed) as passed from (
                          select bool_or(r.status <> 'SKIPPED') as entered, bool_and(r.status in ('PASSED', 'SKIPPED')) and bool_or(r.status = 'PASSED') as passed
                          from application_stage_results r join recruitment_stages s on s.id = r.stage_id
                          join job_applications a on a.id = r.application_id
                          join recruitment_positions p on p.id = a.position_id
                          join recruitment_campaigns c on c.id = p.campaign_id""" + appWhere + """
                           and s.stage_type = :fType group by a.id) x""", params, rs -> {
                    if (rs.getInt("entered") > 0) steps.add(step(type[0], type[1], rs.getInt("entered"), rs.getInt("passed")));
                });
            }
        }
        steps.add(new FunnelStep("HIRED", "Đã tuyển", kpis.hired(), kpis.hired(),
                kpis.totalApplications() == 0 ? null : round(100.0 * kpis.hired() / kpis.totalApplications())));
        return steps;
    }

    private static FunnelStep step(String key, String label, int entered, int passed) {
        return new FunnelStep(key, label, entered, passed, entered == 0 ? null : round(100.0 * passed / entered));
    }

    private List<SourceStat> sources(MapSqlParameterSource params, String appWhere) {
        return jdbc.query("""
                select a.source, count(*) as applications,
                       count(*) filter (where a.status not in ('NEW', 'SCREENING_PENDING', 'REJECTED', 'WITHDRAWN')
                           and exists (select 1 from application_stage_results r join recruitment_stages s on s.id = r.stage_id
                                       where r.application_id = a.id and s.stage_type = 'SCREENING' and r.status = 'PASSED')) as passed_screening,
                       count(*) filter (where exists (select 1 from recruitment_offers o where o.application_id = a.id
                                                      and o.status in ('SENT', 'ACCEPTED', 'DECLINED', 'EXPIRED'))) as offers,
                       count(*) filter (where a.status = 'HIRED') as hired""" + APPS + appWhere + """
                 group by a.source order by applications desc""", params, (rs, i) -> new SourceStat(rs.getString("source"),
                rs.getInt("applications"), rs.getInt("passed_screening"), rs.getInt("offers"), rs.getInt("hired"),
                rs.getInt("applications") == 0 ? null : round(100.0 * rs.getInt("hired") / rs.getInt("applications"))));
    }

    private List<ProgressRow> campaignProgress(MapSqlParameterSource params, String campaignWhere) {
        return jdbc.query("""
                select c.id, c.code, c.name, st.name as store_name, c.status, c.end_date,
                       (select coalesce(sum(p.headcount), 0) from recruitment_positions p where p.campaign_id = c.id) as headcount,
                       (select count(*) from job_applications a join recruitment_positions p on p.id = a.position_id
                        where p.campaign_id = c.id and a.status = 'HIRED') as hired,
                       (select count(*) from job_applications a join recruitment_positions p on p.id = a.position_id
                        where p.campaign_id = c.id) as applications,
                       (select count(*) from job_applications a join recruitment_positions p on p.id = a.position_id
                        where p.campaign_id = c.id and a.status in """ + CampaignService.IN_PROCESS + """
                ) as in_process
                from recruitment_campaigns c left join stores st on st.id = c.store_id""" + campaignWhere + """
                 and c.status in ('OPEN', 'PAUSED') order by c.end_date nulls last, c.created_at desc limit 20""",
                params, (rs, i) -> progress(rs, rs.getString("store_name") == null ? "Văn phòng" : rs.getString("store_name"),
                        rs.getObject("end_date", LocalDate.class)));
    }

    private List<ProgressRow> positionProgress(Filter f, MapSqlParameterSource params, String campaignWhere) {
        return jdbc.query("""
                select p.id, p.code, p.title as name, c.name as campaign_name, p.status, p.deadline, p.headcount,
                       (select count(*) from job_applications a where a.position_id = p.id and a.status = 'HIRED') as hired,
                       (select count(*) from job_applications a where a.position_id = p.id) as applications,
                       (select count(*) from job_applications a where a.position_id = p.id and a.status in """ + CampaignService.IN_PROCESS + """
                ) as in_process
                from recruitment_positions p join recruitment_campaigns c on c.id = p.campaign_id""" + campaignWhere + positionFilter(f) + """
                 and p.status in ('OPEN', 'PAUSED', 'FILLED') and c.status in ('OPEN', 'PAUSED')
                order by p.deadline nulls last, p.created_at desc limit 30""",
                params, (rs, i) -> progress(rs, rs.getString("campaign_name"), rs.getObject("deadline", LocalDate.class)));
    }

    private static ProgressRow progress(java.sql.ResultSet rs, String parent, LocalDate deadline) throws java.sql.SQLException {
        int headcount = rs.getInt("headcount");
        int hired = rs.getInt("hired");
        return new ProgressRow(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"), parent,
                rs.getString("status"), headcount, hired, rs.getInt("applications"), rs.getInt("in_process"), deadline,
                headcount == 0 ? null : round(100.0 * hired / headcount));
    }

    private static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
