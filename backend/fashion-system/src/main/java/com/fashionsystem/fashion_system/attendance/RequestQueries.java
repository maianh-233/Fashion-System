package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.attendance.AttendanceAccess.Actor;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Truy vấn danh sách đơn (giải trình / nghỉ phép / tăng ca) theo phạm vi người xem, và sinh mã đơn. */
@Component
public class RequestQueries {
    private static final DateTimeFormatter YYMM = DateTimeFormatter.ofPattern("yyMM");
    private final NamedParameterJdbcTemplate jdbc;
    private final AttendanceAccess access;

    public RequestQueries(NamedParameterJdbcTemplate jdbc, AttendanceAccess access) {
        this.jdbc = jdbc;
        this.access = access;
    }

    public record Filter(String status, UUID storeId, String employeeType, String keyword, LocalDate from, LocalDate to) {
    }

    NamedParameterJdbcTemplate jdbc() {
        return jdbc;
    }

    /** Mã đơn dạng {@code GT2611-0001}. */
    String nextCode(String prefix, String sequence) {
        Long next = jdbc.queryForObject("select nextval('" + sequence + "')", new MapSqlParameterSource(), Long.class);
        return prefix + LocalDate.now().format(YYMM) + "-" + String.format("%04d", next);
    }

    /**
     * @param dateCondition điều kiện ngày dùng {@code :from} / {@code :to}, chỉ áp dụng khi lọc có đủ hai ngày
     */
    <T> Page<T> page(Actor actor, Filter filter, Pageable pageable, String select, String table, String alias,
                     String dateCondition, RowMapper<T> mapper) {
        String type = StringUtils.hasText(filter.employeeType()) ? filter.employeeType().toUpperCase() : null;
        MapSqlParameterSource params = AttendanceAccess.params(actor, access.storeFilter(actor, filter.storeId()), type)
                .addValue("status", StringUtils.hasText(filter.status()) ? filter.status().toUpperCase() : null, java.sql.Types.VARCHAR)
                .addValue("kw", StringUtils.hasText(filter.keyword()) ? "%" + filter.keyword().trim().toLowerCase() + "%" : null,
                        java.sql.Types.VARCHAR)
                .addValue("from", filter.from()).addValue("to", filter.to());
        String where = " where " + AttendanceAccess.guard(alias + ".user_id", alias + ".store_id")
                + " and (cast(:status as varchar) is null or " + alias + ".status = :status)"
                + " and (cast(:kw as varchar) is null or lower(u.full_name) like :kw or lower(coalesce(u.employee_code, '')) like :kw"
                + " or lower(" + alias + ".code) like :kw)"
                + (filter.from() != null && filter.to() != null ? " and " + dateCondition : "");
        Integer total = jdbc.queryForObject("select count(*) from " + table + " join users u on u.id = " + alias + ".user_id" + where,
                params, Integer.class);
        List<T> rows = jdbc.query(select + where + " order by (" + alias + ".status = 'PENDING') desc, " + alias
                        + ".created_at desc limit :limit offset :offset",
                params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset()), mapper);
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
