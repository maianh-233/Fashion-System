package com.fashionsystem.fashion_system.recruitment;

import com.fashionsystem.fashion_system.entity.EmploymentType;
import com.fashionsystem.fashion_system.recruitment.RecruitmentAccess.Actor;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Lookups;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Option;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.OrgPosition;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.RoleOption;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Scope;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.UserRef;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Danh mục cho form tuyển dụng; nhân viên / cửa hàng được lọc theo phạm vi của người dùng. */
@Service
@Transactional(readOnly = true)
public class RecruitmentLookupService {
    static final Map<String, String> SOURCE_LABELS = Map.of("WEBSITE", "Website tuyển dụng", "REFERRAL", "Giới thiệu nội bộ",
            "LINKEDIN", "LinkedIn", "TOPCV", "TopCV", "VIETNAMWORKS", "VietnamWorks", "FACEBOOK", "Facebook",
            "JOB_FAIR", "Ngày hội việc làm", "HEADHUNTER", "Headhunter", "WALK_IN", "Nộp trực tiếp tại cửa hàng", "OTHER", "Khác");
    private static final List<String> SOURCE_ORDER = List.of("WEBSITE", "WALK_IN", "REFERRAL", "FACEBOOK", "TOPCV",
            "VIETNAMWORKS", "LINKEDIN", "JOB_FAIR", "HEADHUNTER", "OTHER");
    private static final Map<String, String> EMPLOYMENT_LABELS = Map.of("FULL_TIME", "Toàn thời gian", "PART_TIME",
            "Bán thời gian", "CONTRACT", "Hợp đồng", "INTERN", "Thực tập", "TEMPORARY", "Thời vụ");

    private final NamedParameterJdbcTemplate jdbc;

    public RecruitmentLookupService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Lookups lookups(Actor actor) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("store", actor.storeId(), java.sql.Types.OTHER);
        List<Option> departments = jdbc.query("select id, name from departments order by name", params,
                (rs, i) -> new Option(rs.getString("id"), rs.getString("name")));
        List<OrgPosition> positions = jdbc.query("select id, name, department_id from positions order by name", params,
                (rs, i) -> new OrgPosition(rs.getObject("id", UUID.class), rs.getString("name"), rs.getObject("department_id", UUID.class)));
        List<Option> stores = jdbc.query("""
                select id, name from stores where coalesce(active, true) and (cast(:store as uuid) is null or id = :store)
                order by name""", params, (rs, i) -> new Option(rs.getString("id"), rs.getString("name")));
        List<RoleOption> roles = jdbc.query("""
                select code, name from roles where code not in ('SUPER_ADMIN', 'ADMIN') order by name""", params,
                (rs, i) -> new RoleOption(rs.getString("code"), rs.getString("name")));
        return new Lookups(departments, positions, users(actor, null, null), SOURCE_ORDER.stream()
                .map(s -> new Option(s, SOURCE_LABELS.get(s))).toList(),
                Arrays.stream(ApplicationStatus.values()).map(s -> new Option(s.name(), s.label())).toList(),
                Arrays.stream(StageStatus.values()).map(s -> new Option(s.name(), s.label())).toList(),
                Arrays.stream(EmploymentType.values()).map(t -> new Option(t.name(), EMPLOYMENT_LABELS.getOrDefault(t.name(), t.name()))).toList(),
                roles, stores, actor.permissions(), new Scope(actor.storeId(), actor.storeName()));
    }

    /**
     * Nhân viên có thể phân công: nhân viên cấp chuỗi luôn có; nhân viên cửa hàng chỉ khi thuộc {@code storeId} (mặc định
     * cửa hàng của người dùng). {@code storeId} null với người cấp chuỗi = mọi nhân viên.
     */
    public List<UserRef> users(Actor actor, UUID storeId, String keyword) {
        UUID store = actor.global() ? storeId : actor.storeId();
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("store", store, java.sql.Types.OTHER)
                .addValue("kw", keyword == null || keyword.isBlank() ? null : "%" + keyword.trim() + "%", java.sql.Types.VARCHAR);
        return jdbc.query("""
                select u.id, u.full_name, u.username from users u
                where u.deleted_at is null and coalesce(u.active, true) and coalesce(u.locked, false) = false
                  and (cast(:store as uuid) is null
                       or exists (select 1 from store_staffs s where s.user_id = u.id and s.store_id = :store and s.active)
                       or not exists (select 1 from store_staffs s where s.user_id = u.id and s.active))
                  and (cast(:kw as varchar) is null or u.full_name ilike :kw or u.username ilike :kw or u.email ilike :kw)
                order by u.full_name limit 300""", params,
                (rs, i) -> new UserRef(rs.getObject("id", UUID.class), rs.getString("full_name"), rs.getString("username")));
    }
}
