package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.attendance.AttendanceAccess.Actor;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.Template;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.TemplateRequest;
import com.fashionsystem.fashion_system.audit.BusinessAudit;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Ca mẫu (bảng {@code shifts}): ca dùng chung toàn chuỗi (store_id null, chỉ cấp chuỗi sửa) và ca riêng của cửa hàng
 * (quản lý cửa hàng sửa ca của cửa hàng mình). Giờ ca đọc / ghi bằng JDBC vì Hibernate lệch cột TIME 7 giờ.
 */
@Service
@Transactional(readOnly = true)
public class ShiftTemplateService {
    static final Set<String> TYPES = Set.of("MORNING", "AFTERNOON", "EVENING", "FULL_DAY", "NIGHT", "CUSTOM");
    static final Set<String> COLORS = Set.of("amber", "sky", "violet", "emerald", "rose", "teal", "orange", "zinc");

    private final NamedParameterJdbcTemplate jdbc;
    private final AttendanceAccess access;

    public ShiftTemplateService(NamedParameterJdbcTemplate jdbc, AttendanceAccess access) {
        this.jdbc = jdbc;
        this.access = access;
    }

    /** Ca dùng được tại cửa hàng: ca chung + ca riêng cửa hàng. {@code storeId} null với người cấp chuỗi = chỉ ca chung. */
    public List<Template> list(Actor actor, UUID storeId, boolean includeInactive) {
        UUID store = access.storeFilter(actor, storeId);
        return jdbc.query(SELECT + """
                 where (s.store_id is null or s.store_id = cast(:store as uuid)) and (:all or s.active)
                 order by s.active desc, (s.store_id is null), s.sort_order, s.start_time, s.name
                """, new MapSqlParameterSource().addValue("store", store, java.sql.Types.OTHER).addValue("all", includeInactive),
                ShiftTemplateService::map);
    }

    Template get(UUID id) {
        List<Template> rows = jdbc.query(SELECT + " where s.id = :id", new MapSqlParameterSource("id", id), ShiftTemplateService::map);
        if (rows.isEmpty()) throw BusinessException.notFound("Ca mẫu không tồn tại");
        return rows.getFirst();
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Template save(Actor actor, UUID id, TemplateRequest request) {
        Template existing = id == null ? null : get(id);
        UUID storeId = existing != null ? existing.storeId() : request.storeId();
        requireEditable(actor, storeId);
        String type = request.shiftType().trim().toUpperCase();
        if (!TYPES.contains(type)) throw BusinessException.badRequest("Loại ca không hợp lệ");
        if (request.startTime().equals(request.endTime())) throw BusinessException.badRequest("Giờ bắt đầu và kết thúc ca phải khác nhau");
        if (request.breakMinutes() >= spanMinutes(request.startTime(), request.endTime()))
            throw BusinessException.badRequest("Thời gian nghỉ phải ngắn hơn độ dài ca");
        String color = StringUtils.hasText(request.color()) ? request.color().trim().toLowerCase() : "amber";
        if (!COLORS.contains(color)) throw BusinessException.badRequest("Màu ca không hợp lệ");
        String code = request.code().trim().toUpperCase();
        Boolean duplicate = jdbc.queryForObject("""
                select exists (select 1 from shifts where upper(code) = :code and store_id is not distinct from cast(:store as uuid)
                               and (cast(:id as uuid) is null or id <> :id))
                """, new MapSqlParameterSource("code", code).addValue("store", storeId, java.sql.Types.OTHER)
                .addValue("id", id, java.sql.Types.OTHER), Boolean.class);
        if (Boolean.TRUE.equals(duplicate)) throw BusinessException.conflict("Mã ca " + code + " đã tồn tại");

        MapSqlParameterSource p = new MapSqlParameterSource().addValue("store", storeId, java.sql.Types.OTHER).addValue("code", code)
                .addValue("name", request.name().trim()).addValue("type", type).addValue("start", request.startTime())
                .addValue("end", request.endTime()).addValue("break", request.breakMinutes()).addValue("color", color)
                .addValue("sort", request.sortOrder()).addValue("active", request.active()).addValue("actor", actor.userId());
        if (id == null) {
            id = jdbc.queryForObject("""
                    insert into shifts(store_id, code, name, shift_type, start_time, end_time, break_minutes, color, sort_order, active,
                                       created_by, created_at)
                    values (cast(:store as uuid), :code, :name, :type, :start, :end, :break, :color, :sort, :active, :actor,
                            now() at time zone 'UTC')
                    returning id
                    """, p, UUID.class);
        } else {
            jdbc.update("""
                    update shifts set code = :code, name = :name, shift_type = :type, start_time = :start, end_time = :end,
                           break_minutes = :break, color = :color, sort_order = :sort, active = :active,
                           updated_at = now() at time zone 'UTC'
                    where id = :id
                    """, p.addValue("id", id));
        }
        return get(id);
    }

    /** Ca chung chỉ người cấp chuỗi sửa; ca cửa hàng chỉ người thuộc cửa hàng đó (hoặc cấp chuỗi). */
    private static void requireEditable(Actor actor, UUID storeId) {
        if (storeId == null && !actor.global())
            throw BusinessException.forbidden("Chỉ quản lý cấp chuỗi được sửa ca dùng chung");
        if (storeId != null && !actor.global() && !storeId.equals(actor.storeId()))
            throw BusinessException.forbidden("Bạn chỉ được quản lý ca của cửa hàng mình");
    }

    /** Độ dài ca (phút), ca qua đêm tính sang hôm sau. */
    static int spanMinutes(LocalTime start, LocalTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        return (int) (minutes <= 0 ? minutes + 24 * 60 : minutes);
    }

    static int paidMinutes(LocalTime start, LocalTime end, int breakMinutes) {
        return Math.max(0, spanMinutes(start, end) - breakMinutes);
    }

    private static final String SELECT = """
            select s.id, s.store_id, st.name as store_name, s.code, s.name, s.shift_type, s.start_time, s.end_time,
                   s.break_minutes, s.color, s.sort_order, s.active
            from shifts s left join stores st on st.id = s.store_id
            """;

    private static Template map(ResultSet rs, int i) throws SQLException {
        LocalTime start = rs.getObject("start_time", LocalTime.class);
        LocalTime end = rs.getObject("end_time", LocalTime.class);
        int brk = rs.getInt("break_minutes");
        return new Template(rs.getObject("id", UUID.class), rs.getObject("store_id", UUID.class), rs.getString("store_name"),
                rs.getString("code"), rs.getString("name"), rs.getString("shift_type"), start, end, brk, rs.getString("color"),
                rs.getInt("sort_order"), rs.getBoolean("active"), paidMinutes(start, end, brk));
    }
}
