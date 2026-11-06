package com.fashionsystem.fashion_system.payroll;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.payroll.PayrollAccess.Actor;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.EmployeeSalary;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.EmployeeSalaryDetail;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.HistoryEntry;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.Structure;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.StructureBrief;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.StructureItem;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.StructureItemRequest;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.StructureRequest;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Hồ sơ lương theo phiên bản: mỗi lần thay đổi mức lương / khoản lương tạo phiên bản mới có ngày hiệu lực; phiên bản
 * trước chỉ được đóng ngày kết thúc. Phiên bản đã dùng cho kỳ lương đã gửi duyệt / chốt thì không sửa được. Mọi thay
 * đổi ghi vào salary_history (dữ liệu cũ, dữ liệu mới, lý do, người sửa).
 */
@Service
@Transactional(readOnly = true)
public class SalaryStructureService {
    static final String EMPLOYEE = "u.deleted_at is null and u.employee_code is not null";
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM/yyyy");

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;

    public SalaryStructureService(NamedParameterJdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    // ------------------------------------------------------------------ danh sách

    public Page<EmployeeSalary> list(Actor actor, String keyword, UUID storeId, String filter, Pageable pageable) {
        PayrollAccess.require(actor.permissions().salaryView(), "Bạn không có quyền xem hồ sơ lương");
        MapSqlParameterSource params = PayrollAccess.params(actor)
                .addValue("store", PayrollAccess.storeFilter(actor, storeId), java.sql.Types.OTHER)
                .addValue("kw", StringUtils.hasText(keyword) ? "%" + keyword.trim().toLowerCase() + "%" : null, java.sql.Types.VARCHAR)
                .addValue("today", LocalDate.now())
                .addValue("filter", StringUtils.hasText(filter) ? filter : null, java.sql.Types.VARCHAR);
        String where = " where " + EMPLOYEE + " and " + PayrollAccess.employeeGuard("u") + """
                 and (cast(:store as uuid) is null or exists (select 1 from store_staffs fs where fs.user_id = u.id and fs.active and fs.store_id = :store))
                 and (cast(:kw as varchar) is null or lower(u.full_name) like :kw or lower(u.employee_code) like :kw)
                 and (cast(:filter as varchar) is null
                      or (:filter = 'MISSING' and not exists (select 1 from salary_structures m where m.user_id = u.id
                          and m.effective_from <= :today and (m.effective_to is null or m.effective_to >= :today)))
                      or (:filter = 'HAS' and exists (select 1 from salary_structures m where m.user_id = u.id
                          and m.effective_from <= :today and (m.effective_to is null or m.effective_to >= :today))))
                """;
        Integer total = jdbc.queryForObject("select count(*) from users u" + where, params, Integer.class);
        List<EmployeeSalary> rows = jdbc.query(EMPLOYEE_SELECT + where + " order by u.full_name limit :limit offset :offset",
                params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset()),
                (rs, i) -> mapEmployee(rs, actor.admin()));
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    private static final String EMPLOYEE_SELECT = """
            select u.id, u.employee_code, u.full_name, u.hire_date, u.termination_date, u.employment_status,
                   ss.store_id, st.name as store_name, pos.name as position_name,
                   (select string_agg(d.name, ', ' order by d.name) from user_departments ud join departments d on d.id = ud.department_id
                    where ud.user_id = u.id) as department_name,
                   cur.id as cur_id, cur.base_salary as cur_base, cur.effective_from as cur_from, cur.effective_to as cur_to,
                   cur.pay_method as cur_method, cur.confidential as cur_confidential,
                   (select count(*) from salary_structures v where v.user_id = u.id) as versions
            from users u
            left join store_staffs ss on ss.user_id = u.id and ss.active
            left join stores st on st.id = ss.store_id
            left join positions pos on pos.id = u.position_id
            left join lateral (select * from salary_structures c where c.user_id = u.id
                               and c.effective_from <= :today and (c.effective_to is null or c.effective_to >= :today)
                               order by c.effective_from desc limit 1) cur on true
            """;

    private static EmployeeSalary mapEmployee(ResultSet rs, boolean admin) throws SQLException {
        StructureBrief current = null;
        if (rs.getObject("cur_id") != null) {
            boolean hidden = rs.getBoolean("cur_confidential") && !admin;
            current = new StructureBrief(rs.getObject("cur_id", UUID.class), hidden ? null : rs.getBigDecimal("cur_base"),
                    rs.getObject("cur_from", LocalDate.class), rs.getObject("cur_to", LocalDate.class), rs.getString("cur_method"),
                    rs.getBoolean("cur_confidential"));
        }
        UUID storeId = rs.getObject("store_id", UUID.class);
        return new EmployeeSalary(rs.getObject("id", UUID.class), rs.getString("employee_code"), rs.getString("full_name"),
                storeId == null ? "GLOBAL" : "STORE", storeId, rs.getString("store_name"), rs.getString("department_name"),
                rs.getString("position_name"), rs.getObject("hire_date", LocalDate.class), rs.getObject("termination_date", LocalDate.class),
                rs.getString("employment_status"), current, rs.getInt("versions"));
    }

    // ------------------------------------------------------------------ chi tiết

    public EmployeeSalaryDetail detail(Actor actor, UUID userId) {
        PayrollAccess.require(actor.permissions().salaryView(), "Bạn không có quyền xem hồ sơ lương");
        EmployeeSalary employee = employee(actor, userId);
        List<Structure> structures = structures(userId);
        if (!actor.admin() && structures.stream().anyMatch(Structure::confidential))
            throw BusinessException.forbidden("Hồ sơ lương này được bảo mật, chỉ quản trị lương được xem");
        List<HistoryEntry> history = jdbc.query("""
                select h.action, h.reason, a.full_name, h.changed_at, h.old_data::text as old_data, h.new_data::text as new_data
                from salary_history h left join users a on a.id = h.changed_by
                where h.user_id = :u order by h.changed_at desc
                """, new MapSqlParameterSource("u", userId), (rs, i) -> new HistoryEntry(rs.getString("action"), rs.getString("reason"),
                rs.getString("full_name"), DbTimes.fromDb(rs.getTimestamp("changed_at")), parse(rs.getString("old_data")),
                parse(rs.getString("new_data"))));
        return new EmployeeSalaryDetail(employee, structures, history);
    }

    EmployeeSalary employee(Actor actor, UUID userId) {
        List<EmployeeSalary> rows = jdbc.query(EMPLOYEE_SELECT + " where u.id = :u and " + EMPLOYEE + " and " + PayrollAccess.employeeGuard("u"),
                PayrollAccess.params(actor).addValue("u", userId).addValue("today", LocalDate.now()), (rs, i) -> mapEmployee(rs, actor.admin()));
        if (rows.isEmpty()) throw BusinessException.notFound("Nhân viên không tồn tại hoặc không thuộc phạm vi của bạn");
        return rows.getFirst();
    }

    /** Các phiên bản lương của nhân viên (mới nhất trước), kèm khoản lương. */
    List<Structure> structures(UUID userId) {
        List<Structure> rows = jdbc.query("""
                select s.*, c.full_name as created_by_name,
                       exists (select 1 from payroll_records r join payroll_periods p on p.id = r.period_id
                               where r.user_id = s.user_id and r.active and p.status in ('PENDING_APPROVAL', 'APPROVED', 'PAID')
                                 and r.window_start <= coalesce(s.effective_to, date '9999-12-31') and r.window_end >= s.effective_from) as locked
                from salary_structures s left join users c on c.id = s.created_by
                where s.user_id = :u order by s.effective_from desc
                """, new MapSqlParameterSource("u", userId), (rs, i) -> new Structure(rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class), rs.getBigDecimal("base_salary"), rs.getObject("effective_from", LocalDate.class),
                rs.getObject("effective_to", LocalDate.class), rs.getString("pay_method"), rs.getString("bank_name"),
                rs.getString("bank_account"), rs.getBoolean("overtime_eligible"), rs.getBigDecimal("overtime_hourly_rate"),
                rs.getBoolean("confidential"), rs.getString("note"), List.of(), rs.getString("created_by_name"),
                DbTimes.fromDb(rs.getTimestamp("created_at")), rs.getBoolean("locked")));
        return rows.stream().map(s -> new Structure(s.id(), s.userId(), s.baseSalary(), s.effectiveFrom(), s.effectiveTo(), s.payMethod(),
                s.bankName(), s.bankAccount(), s.overtimeEligible(), s.overtimeHourlyRate(), s.confidential(), s.note(), items(s.id()),
                s.createdByName(), s.createdAt(), s.locked())).toList();
    }

    List<StructureItem> items(UUID structureId) {
        return jdbc.query("""
                select i.component_id, c.code, c.name, c.kind, c.calc_method, i.amount, i.rate, i.note
                from salary_structure_items i join payroll_components c on c.id = i.component_id
                where i.structure_id = :id order by c.sort_order, c.name
                """, new MapSqlParameterSource("id", structureId), (rs, i) -> new StructureItem(rs.getObject("component_id", UUID.class),
                rs.getString("code"), rs.getString("name"), rs.getString("kind"), rs.getString("calc_method"), rs.getBigDecimal("amount"),
                rs.getBigDecimal("rate"), rs.getString("note")));
    }

    // ------------------------------------------------------------------ tạo / sửa

    @Transactional
    public Structure create(Actor actor, UUID userId, StructureRequest request) {
        requireManage(actor, userId);
        lockUser(userId);
        requireOpen(userId, request.effectiveFrom());
        validate(actor, request);
        List<Structure> existing = structures(userId);
        if (!actor.admin() && existing.stream().anyMatch(Structure::confidential))
            throw BusinessException.forbidden("Hồ sơ lương này được bảo mật, chỉ quản trị lương được thay đổi");
        for (Structure s : existing) {
            if (!s.effectiveFrom().isBefore(request.effectiveFrom()))
                throw BusinessException.conflict("Đã có phiên bản lương hiệu lực từ " + s.effectiveFrom()
                        + ". Phiên bản mới phải có ngày hiệu lực sau ngày đó, hoặc sửa phiên bản chưa chốt.");
        }
        Structure previous = existing.stream()
                .filter(s -> s.effectiveTo() == null || !s.effectiveTo().isBefore(request.effectiveFrom()))
                .findFirst().orElse(null);
        if (previous != null) {
            LocalDate closeAt = request.effectiveFrom().minusDays(1);
            jdbc.update("update salary_structures set effective_to = :to, updated_at = now() at time zone 'UTC' where id = :id",
                    new MapSqlParameterSource("to", closeAt).addValue("id", previous.id()));
            history(userId, previous.id(), "CLOSE", Map.of("effectiveTo", String.valueOf(previous.effectiveTo())),
                    Map.of("effectiveTo", closeAt.toString()), request.reason().trim(), actor.userId());
        }
        UUID id = jdbc.queryForObject("""
                insert into salary_structures(user_id, base_salary, effective_from, effective_to, pay_method, bank_name, bank_account,
                                              overtime_eligible, overtime_hourly_rate, confidential, note, created_by)
                values (:u, :base, :from, :to, :method, :bank, :account, :ot, :otRate, :confidential, :note, :actor) returning id
                """, structureParams(request).addValue("u", userId).addValue("actor", actor.userId()), UUID.class);
        saveItems(id, request.items());
        Structure created = structures(userId).stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
        history(userId, id, existing.isEmpty() ? "CREATE" : "NEW_VERSION", previous == null ? null : snapshot(previous),
                snapshot(created), request.reason().trim(), actor.userId());
        return created;
    }

    @Transactional
    public Structure update(Actor actor, UUID structureId, StructureRequest request) {
        UUID userId = userOf(structureId);
        requireManage(actor, userId);
        lockUser(userId);
        validate(actor, request);
        List<Structure> all = structures(userId);
        Structure before = all.stream().filter(s -> s.id().equals(structureId)).findFirst().orElseThrow();
        if (before.confidential() && !actor.admin())
            throw BusinessException.forbidden("Hồ sơ lương này được bảo mật, chỉ quản trị lương được thay đổi");
        if (before.locked())
            throw BusinessException.conflict("Phiên bản lương này đã dùng cho kỳ lương đã gửi duyệt / chốt. Hãy tạo phiên bản mới có hiệu lực từ kỳ sau.");
        requireOpen(userId, request.effectiveFrom());
        LocalDate end = request.effectiveTo() == null ? LocalDate.MAX : request.effectiveTo();
        for (Structure s : all) {
            if (s.id().equals(structureId)) continue;
            LocalDate otherEnd = s.effectiveTo() == null ? LocalDate.MAX : s.effectiveTo();
            if (!s.effectiveFrom().isAfter(end) && !otherEnd.isBefore(request.effectiveFrom()))
                throw BusinessException.conflict("Khoảng hiệu lực trùng với phiên bản từ " + s.effectiveFrom());
        }
        jdbc.update("""
                update salary_structures set base_salary = :base, effective_from = :from, effective_to = :to, pay_method = :method,
                       bank_name = :bank, bank_account = :account, overtime_eligible = :ot, overtime_hourly_rate = :otRate,
                       confidential = :confidential, note = :note, updated_at = now() at time zone 'UTC'
                where id = :id
                """, structureParams(request).addValue("id", structureId));
        jdbc.update("delete from salary_structure_items where structure_id = :id", new MapSqlParameterSource("id", structureId));
        saveItems(structureId, request.items());
        Structure after = structures(userId).stream().filter(s -> s.id().equals(structureId)).findFirst().orElseThrow();
        history(userId, structureId, "UPDATE", snapshot(before), snapshot(after), request.reason().trim(), actor.userId());
        return after;
    }

    private void requireManage(Actor actor, UUID userId) {
        PayrollAccess.require(actor.permissions().salaryManage(), "Bạn không có quyền cập nhật hồ sơ lương");
        employee(actor, userId);
        if (actor.userId().equals(userId)) throw BusinessException.forbidden("Không thể tự thay đổi hồ sơ lương của chính mình");
    }

    /** Khóa theo nhân viên để hai thao tác đồng thời không tạo phiên bản chồng nhau. */
    private void lockUser(UUID userId) {
        jdbc.queryForList("select id from users where id = :u for update", new MapSqlParameterSource("u", userId), UUID.class);
    }

    /** Không cho hiệu lực rơi vào tháng đã gửi duyệt / chốt lương (không thay đổi bảng lương đã duyệt). */
    private void requireOpen(UUID userId, LocalDate effectiveFrom) {
        List<LocalDate> closed = jdbc.queryForList("""
                select max(r.window_end) from payroll_records r join payroll_periods p on p.id = r.period_id
                where r.user_id = :u and r.active and p.status in ('PENDING_APPROVAL', 'APPROVED', 'PAID')
                """, new MapSqlParameterSource("u", userId), LocalDate.class);
        LocalDate last = closed.isEmpty() ? null : closed.getFirst();
        if (last != null && !effectiveFrom.isAfter(last))
            throw BusinessException.conflict("Lương đã chốt đến tháng " + last.format(MONTH) + ". Ngày hiệu lực phải sau "
                    + last + "; sai lệch của kỳ đã chốt hãy tạo điều chỉnh lương.");
    }

    private void validate(Actor actor, StructureRequest request) {
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom()))
            throw BusinessException.badRequest("Ngày kết thúc phải từ ngày hiệu lực trở đi");
        if (request.confidential() && !actor.admin())
            throw BusinessException.forbidden("Chỉ quản trị lương được đánh dấu hồ sơ bảo mật");
        if ("BANK_TRANSFER".equals(request.payMethod()) && !StringUtils.hasText(request.bankAccount()))
            throw BusinessException.badRequest("Trả lương chuyển khoản cần số tài khoản");
        Set<UUID> seen = new HashSet<>();
        for (StructureItemRequest item : request.items() == null ? List.<StructureItemRequest>of() : request.items()) {
            if (!seen.add(item.componentId())) throw BusinessException.badRequest("Một khoản lương chỉ được thêm một lần");
            List<Map<String, Object>> comp = jdbc.queryForList("select calc_method, active, name from payroll_components where id = :id",
                    new MapSqlParameterSource("id", item.componentId()));
            if (comp.isEmpty() || !Boolean.TRUE.equals(comp.getFirst().get("active")))
                throw BusinessException.badRequest("Khoản lương không tồn tại hoặc đã ngừng sử dụng");
            String method = (String) comp.getFirst().get("calc_method");
            if ("PERCENT_OF_BASE".equals(method) && item.rate() == null)
                throw BusinessException.badRequest("Khoản " + comp.getFirst().get("name") + " cần tỷ lệ %");
            if (!"PERCENT_OF_BASE".equals(method) && item.amount() == null)
                throw BusinessException.badRequest("Khoản " + comp.getFirst().get("name") + " cần số tiền");
        }
    }

    private MapSqlParameterSource structureParams(StructureRequest r) {
        return new MapSqlParameterSource("base", r.baseSalary()).addValue("from", r.effectiveFrom()).addValue("to", r.effectiveTo())
                .addValue("method", r.payMethod()).addValue("bank", trim(r.bankName())).addValue("account", trim(r.bankAccount()))
                .addValue("ot", r.overtimeEligible()).addValue("otRate", r.overtimeHourlyRate())
                .addValue("confidential", r.confidential()).addValue("note", trim(r.note()));
    }

    private void saveItems(UUID structureId, List<StructureItemRequest> items) {
        if (items == null) return;
        for (StructureItemRequest item : items) {
            jdbc.update("""
                    insert into salary_structure_items(structure_id, component_id, amount, rate, note) values (:s, :c, :amount, :rate, :note)
                    """, new MapSqlParameterSource("s", structureId).addValue("c", item.componentId()).addValue("amount", item.amount())
                    .addValue("rate", item.rate()).addValue("note", trim(item.note())));
        }
    }

    private UUID userOf(UUID structureId) {
        List<UUID> ids = jdbc.queryForList("select user_id from salary_structures where id = :id",
                new MapSqlParameterSource("id", structureId), UUID.class);
        if (ids.isEmpty()) throw BusinessException.notFound("Phiên bản lương không tồn tại");
        return ids.getFirst();
    }

    private Map<String, Object> snapshot(Structure s) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("baseSalary", s.baseSalary());
        data.put("effectiveFrom", String.valueOf(s.effectiveFrom()));
        data.put("effectiveTo", s.effectiveTo() == null ? null : s.effectiveTo().toString());
        data.put("payMethod", s.payMethod());
        data.put("overtimeEligible", s.overtimeEligible());
        data.put("overtimeHourlyRate", s.overtimeHourlyRate());
        data.put("confidential", s.confidential());
        data.put("items", s.items().stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", i.name());
            m.put("amount", i.amount());
            m.put("rate", i.rate());
            return m;
        }).toList());
        return data;
    }

    private void history(UUID userId, UUID structureId, String action, Map<String, Object> before, Map<String, Object> after,
                         String reason, UUID actor) {
        jdbc.update("""
                insert into salary_history(user_id, structure_id, action, old_data, new_data, reason, changed_by)
                values (:u, :s, :a, cast(:old as jsonb), cast(:new as jsonb), :reason, :actor)
                """, new MapSqlParameterSource("u", userId).addValue("s", structureId).addValue("a", action)
                .addValue("old", before == null ? null : json.writeValueAsString(before))
                .addValue("new", after == null ? null : json.writeValueAsString(after))
                .addValue("reason", reason).addValue("actor", actor));
    }

    private JsonNode parse(String text) {
        return text == null ? null : json.readTree(text);
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    // ------------------------------------------------------------------ dùng khi tính lương

    record Version(UUID id, BigDecimal baseSalary, LocalDate from, LocalDate to, boolean overtimeEligible, BigDecimal overtimeHourlyRate,
                   String payMethod, String bankName, String bankAccount, boolean confidential) {
    }

    /** Phiên bản lương giao với khoảng [from, to], cũ trước. */
    List<Version> versions(UUID userId, LocalDate from, LocalDate to) {
        return jdbc.query("""
                select * from salary_structures where user_id = :u and effective_from <= :to
                  and (effective_to is null or effective_to >= :from) order by effective_from
                """, new MapSqlParameterSource("u", userId).addValue("from", from).addValue("to", to),
                (rs, i) -> new Version(rs.getObject("id", UUID.class), rs.getBigDecimal("base_salary"),
                        rs.getObject("effective_from", LocalDate.class), rs.getObject("effective_to", LocalDate.class),
                        rs.getBoolean("overtime_eligible"), rs.getBigDecimal("overtime_hourly_rate"), rs.getString("pay_method"),
                        rs.getString("bank_name"), rs.getString("bank_account"), rs.getBoolean("confidential")));
    }
}
