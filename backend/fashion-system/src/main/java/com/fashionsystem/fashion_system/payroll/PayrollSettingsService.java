package com.fashionsystem.fashion_system.payroll;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.payroll.PayrollAccess.Actor;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.Component;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.ComponentRequest;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.Lookups;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.Settings;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.StoreRef;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Chính sách lương chung (ngày công chuẩn, hệ số tăng ca, làm tròn) và danh mục khoản lương. */
@Service
@Transactional(readOnly = true)
public class PayrollSettingsService {
    private static final Set<Integer> ROUNDING_UNITS = Set.of(1, 10, 100, 500, 1000);
    private final NamedParameterJdbcTemplate jdbc;

    public PayrollSettingsService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Lookups lookups(Actor actor) {
        List<StoreRef> stores = jdbc.query("select id, name from stores where active and (:global or id = :store) order by name",
                new MapSqlParameterSource("global", actor.global()).addValue("store", actor.storeId(), java.sql.Types.OTHER),
                (rs, i) -> new StoreRef(rs.getObject("id", UUID.class), rs.getString("name")));
        return new Lookups(actor.permissions(), actor.storeId(), actor.storeName(), stores, components(false), settings());
    }

    public Settings settings() {
        return jdbc.queryForObject("select * from payroll_settings where id = 1", new MapSqlParameterSource(),
                (rs, i) -> new Settings(rs.getString("standard_days_mode"), rs.getBigDecimal("fixed_standard_days"),
                        rs.getBigDecimal("ot_weekday_rate"), rs.getBigDecimal("ot_weekend_rate"), rs.getBigDecimal("ot_holiday_rate"),
                        rs.getInt("rounding_unit"), rs.getString("rounding_mode")));
    }

    @Transactional
    public Settings updateSettings(Actor actor, Settings request) {
        requireGlobalAdmin(actor);
        if (!ROUNDING_UNITS.contains(request.roundingUnit())) throw BusinessException.badRequest("Đơn vị làm tròn không hợp lệ");
        jdbc.update("""
                update payroll_settings set standard_days_mode = :mode, fixed_standard_days = :fixed, ot_weekday_rate = :wd,
                       ot_weekend_rate = :we, ot_holiday_rate = :hol, rounding_unit = :unit, rounding_mode = :rmode,
                       updated_by = :actor, updated_at = now() at time zone 'UTC'
                where id = 1
                """, new MapSqlParameterSource("mode", request.standardDaysMode()).addValue("fixed", request.fixedStandardDays())
                .addValue("wd", request.otWeekdayRate()).addValue("we", request.otWeekendRate()).addValue("hol", request.otHolidayRate())
                .addValue("unit", request.roundingUnit()).addValue("rmode", request.roundingMode()).addValue("actor", actor.userId()));
        return settings();
    }

    public List<Component> components(boolean activeOnly) {
        return jdbc.query("select * from payroll_components" + (activeOnly ? " where active" : "") + " order by sort_order, name",
                new MapSqlParameterSource(), PayrollSettingsService::mapComponent);
    }

    @Transactional
    public Component saveComponent(Actor actor, UUID id, ComponentRequest request) {
        requireGlobalAdmin(actor);
        if ("PERCENT_OF_BASE".equals(request.calcMethod()) && request.defaultRate() == null)
            throw BusinessException.badRequest("Khoản tính theo % lương cơ bản cần nhập tỷ lệ");
        String code = request.code().trim().toUpperCase();
        MapSqlParameterSource params = new MapSqlParameterSource("code", code).addValue("name", request.name().trim())
                .addValue("kind", request.kind()).addValue("method", request.calcMethod())
                .addValue("amount", request.defaultAmount()).addValue("rate", request.defaultRate())
                .addValue("active", request.active()).addValue("note", StringUtils.hasText(request.note()) ? request.note().trim() : null)
                .addValue("sort", request.sortOrder()).addValue("actor", actor.userId());
        try {
            if (id == null) {
                id = jdbc.queryForObject("""
                        insert into payroll_components(code, name, kind, calc_method, default_amount, default_rate, active, note,
                                                       sort_order, created_by)
                        values (:code, :name, :kind, :method, :amount, :rate, :active, :note, :sort, :actor) returning id
                        """, params, UUID.class);
            } else {
                // Đổi cách tính / loại của khoản đang dùng sẽ làm sai lệch kỳ lương cũ khi tính lại: chỉ cho đổi tên, mức, trạng thái.
                int updated = jdbc.update("""
                        update payroll_components set name = :name, default_amount = :amount, default_rate = :rate, active = :active,
                               note = :note, sort_order = :sort, updated_at = now() at time zone 'UTC'
                        where id = :id and code = :code and kind = :kind and calc_method = :method
                        """, params.addValue("id", id));
                if (updated == 0) throw BusinessException.badRequest("Không đổi được mã, loại hoặc cách tính của khoản lương đã tạo");
            }
        } catch (DuplicateKeyException e) {
            throw BusinessException.conflict("Mã khoản lương đã tồn tại");
        }
        return jdbc.queryForObject("select * from payroll_components where id = :id", new MapSqlParameterSource("id", id),
                PayrollSettingsService::mapComponent);
    }

    static Component mapComponent(ResultSet rs, int i) throws SQLException {
        return new Component(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"), rs.getString("kind"),
                rs.getString("calc_method"), rs.getBigDecimal("default_amount"), rs.getBigDecimal("default_rate"),
                rs.getBoolean("active"), rs.getString("note"), rs.getInt("sort_order"));
    }

    private static void requireGlobalAdmin(Actor actor) {
        if (!actor.globalAdmin()) throw BusinessException.forbidden("Chỉ quản trị lương cấp chuỗi được thay đổi chính sách lương");
    }
}
