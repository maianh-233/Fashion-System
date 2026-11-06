package com.fashionsystem.fashion_system.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Xếp ca, công bố, đổi / nhường ca và đối chiếu chấm công trên cơ sở dữ liệu thật; rollback sau mỗi test. */
@SpringBootTest
@Transactional
class ShiftSchedulingIntegrationTest {
    @Autowired ShiftScheduleService schedules;
    @Autowired ShiftSwapService swaps;
    @Autowired ShiftTemplateService templates;
    @Autowired AttendancePolicy policy;
    @Autowired JdbcTemplate jdbc;

    final String tag = UUID.randomUUID().toString().substring(0, 6);
    final UUID storeA = UUID.randomUUID();
    final UUID storeB = UUID.randomUUID();
    final UUID manager = UUID.randomUUID();
    final UUID staff1 = UUID.randomUUID();
    final UUID staff2 = UUID.randomUUID();
    final UUID staffB = UUID.randomUUID();
    final UUID managerOfB = UUID.randomUUID();
    final BigDecimal latA = new BigDecimal("12.").add(BigDecimal.valueOf(Math.abs(UUID.randomUUID().getLeastSignificantBits() % 400000), 6));
    /** Tuần sau (thứ Hai) — luôn ở tương lai và chưa bị khóa lương. */
    final LocalDate monday = LocalDate.now().plusWeeks(1).with(DayOfWeek.MONDAY);
    AttendanceAccess.Actor managerA;
    AttendanceAccess.Actor managerB;
    UUID morning;
    UUID afternoon;

    @BeforeEach
    void setUp() {
        store(storeA, "A", latA);
        store(storeB, "B", latA.add(new BigDecimal("0.5")));
        for (UUID u : new UUID[]{manager, staff1, staff2, staffB, managerOfB}) user(u, u.toString().substring(0, 4));
        staff(manager, storeA);
        staff(staff1, storeA);
        staff(staff2, storeA);
        staff(staffB, storeB);
        staff(managerOfB, storeB);
        managerA = new AttendanceAccess.Actor(manager, storeA, "A", true, true, true, true, false);
        managerB = new AttendanceAccess.Actor(managerOfB, storeB, "B", true, true, true, true, false);
        morning = job(() -> templates.save(managerA, null, new ShiftDtos.TemplateRequest(storeA, "S" + tag, "Sáng", "MORNING",
                LocalTime.of(7, 0), LocalTime.of(13, 0), 0, "sky", 1, true))).id();
        afternoon = job(() -> templates.save(managerA, null, new ShiftDtos.TemplateRequest(storeA, "C" + tag, "Chiều", "AFTERNOON",
                LocalTime.of(14, 0), LocalTime.of(22, 0), 30, "violet", 2, true))).id();
    }

    @Test
    void publishedScheduleDrivesExpectedHoursButDraftDoesNot() {
        LocalDate day = monday.plusDays(1);
        AttendancePolicy.Rule before = policy.ruleFor(staff1, storeA, day);
        ShiftDtos.Entry entry = assign(staff1, day, afternoon);
        assertThat(entry.status()).isEqualTo("DRAFT");
        assertThat(entry.paidMinutes()).isEqualTo(450);
        assertThat(policy.ruleFor(staff1, storeA, day).start()).isEqualTo(before.start());    // nháp chưa có hiệu lực

        ShiftDtos.BulkResult published = job(() -> schedules.publish(managerA, new ShiftDtos.PublishRequest(storeA, monday, monday.plusDays(6))));
        assertThat(published.affected()).isEqualTo(1);
        AttendancePolicy.Rule rule = policy.ruleFor(staff1, storeA, day);
        assertThat(rule.start()).isEqualTo(LocalTime.of(14, 0));
        assertThat(rule.end()).isEqualTo(LocalTime.of(22, 0));
        assertThat(rule.shiftId()).isEqualTo(afternoon);
        assertThat(rule.lateMinutes(LocalTime.of(14, 20))).isEqualTo(20);

        assertThat(schedules.mine(staff1, monday, monday.plusDays(6))).singleElement()
                .satisfies(e -> assertThat(e.actualStatus()).isEqualTo("UPCOMING"));
    }

    @Test
    void schedulingRespectsStoreScopeAndConflicts() {
        LocalDate day = monday.plusDays(2);
        // Quản lý cửa hàng B không xếp ca tại A; nhân viên cửa hàng B không xếp được vào A.
        assertThatThrownBy(() -> job(() -> schedules.create(managerB, request(staff1, day, morning))))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> job(() -> schedules.create(managerA, request(staffB, day, morning))))
                .hasMessageContaining("cửa hàng khác");
        assertThatThrownBy(() -> job(() -> schedules.create(managerA, request(staff1, LocalDate.now().minusDays(1), morning))))
                .hasMessageContaining("đã qua");

        assign(staff1, day, morning);
        assertThatThrownBy(() -> job(() -> schedules.create(managerA, request(staff1, day, afternoon))))
                .hasMessageContaining("đã có ca");

        UUID annual = jdbc.queryForObject("select id from leave_types where code = 'ANNUAL'", UUID.class);
        jdbc.update("""
                insert into leave_requests(code, user_id, leave_type_id, start_date, end_date, status, total_days, created_at)
                values (?, ?, ?, ?, ?, 'APPROVED', 1, now())
                """, "ZQ" + tag, staff2, annual, day, day);
        assertThatThrownBy(() -> job(() -> schedules.create(managerA, request(staff2, day, morning))))
                .hasMessageContaining("nghỉ phép");

        // Ca mẫu của cửa hàng khác không dùng được.
        UUID otherStoreShift = job(() -> templates.save(managerB, null, new ShiftDtos.TemplateRequest(storeB, "B" + tag, "Ca B",
                "MORNING", LocalTime.of(9, 0), LocalTime.of(15, 0), 0, "teal", 1, true))).id();
        assertThatThrownBy(() -> job(() -> schedules.create(managerA, request(staff2, day.plusDays(1), otherStoreShift))))
                .hasMessageContaining("cửa hàng khác");
    }

    @Test
    void copyWeekCreatesDraftsAndSkipsConflicts() {
        assign(staff1, monday, morning);
        assign(staff2, monday.plusDays(1), afternoon);
        LocalDate nextMonday = monday.plusWeeks(1);
        assign(staff1, nextMonday, afternoon);                          // đã có ca → bỏ qua
        ShiftDtos.BulkResult result = job(() -> schedules.copyWeek(managerA, new ShiftDtos.CopyWeekRequest(storeA, monday, nextMonday)));
        assertThat(result.affected()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        ShiftDtos.Grid grid = schedules.grid(managerA, storeA, nextMonday.plusDays(3));
        assertThat(grid.from()).isEqualTo(nextMonday);
        assertThat(grid.entries()).hasSize(2).allSatisfy(e -> assertThat(e.status()).isEqualTo("DRAFT"));
        assertThat(grid.stats().drafts()).isEqualTo(2);
        assertThat(grid.employees()).extracting(ShiftDtos.GridEmployee::id).contains(staff1, staff2, manager);
    }

    @Test
    void giveAwayNeedsPeerAcceptanceAndManagerApproval() {
        LocalDate day = monday.plusDays(3);
        UUID entry = assign(staff1, day, morning).id();
        publishWeek();

        ShiftDtos.Swap swap = job(() -> swaps.create(staff1, new ShiftDtos.SwapCreate(entry, staff2, null, "Việc gia đình")));
        assertThat(swap.swapType()).isEqualTo("GIVE_AWAY");
        assertThat(swap.status()).isEqualTo("PENDING_PEER");
        assertThatThrownBy(() -> job(() -> swaps.create(staff1, new ShiftDtos.SwapCreate(entry, staff2, null, "Lần 2"))))
                .hasMessageContaining("đang có yêu cầu");
        // Chỉ người được nhờ mới đồng ý; quản lý chưa duyệt được khi đồng nghiệp chưa đồng ý.
        assertThatThrownBy(() -> job(() -> swaps.accept(manager, swap.id(), null))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> job(() -> swaps.approve(managerA, swap.id(), null))).hasMessageContaining("bước");

        job(() -> swaps.accept(staff2, swap.id(), new ShiftDtos.PeerResponse("OK")));
        AttendanceAccess.Actor selfApprove = new AttendanceAccess.Actor(staff2, storeA, "A", true, true, true, true, false);
        assertThatThrownBy(() -> job(() -> swaps.approve(selfApprove, swap.id(), null))).hasMessageContaining("chính mình");
        assertThatThrownBy(() -> job(() -> swaps.approve(managerB, swap.id(), null))).isInstanceOf(BusinessException.class);
        assertThat(swaps.list(managerA, new ShiftSwapService.Filter("PENDING_APPROVAL", null, null), Pageable.ofSize(10)).getContent())
                .extracting(ShiftDtos.Swap::id).contains(swap.id());

        ShiftDtos.Swap approved = job(() -> swaps.approve(managerA, swap.id(), new AttendanceDtos.Decision("Đồng ý")));
        assertThat(approved.status()).isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject("select user_id from shift_schedules where id = ?", UUID.class, entry)).isEqualTo(staff2);
        assertThat(policy.ruleFor(staff2, storeA, day).start()).isEqualTo(LocalTime.of(7, 0));
    }

    @Test
    void swapAcrossDaysAndSameDayExchangesShifts() {
        LocalDate d1 = monday.plusDays(1);
        LocalDate d2 = monday.plusDays(2);
        UUID mine = assign(staff1, d1, morning).id();
        UUID theirs = assign(staff2, d2, afternoon).id();
        UUID mineSameDay = assign(staff1, d2.plusDays(2), morning).id();
        UUID theirsSameDay = assign(staff2, d2.plusDays(2), afternoon).id();
        publishWeek();

        ShiftDtos.Swap cross = job(() -> swaps.create(staff1, new ShiftDtos.SwapCreate(mine, staff2, theirs, "Đổi ngày")));
        job(() -> swaps.accept(staff2, cross.id(), null));
        job(() -> swaps.approve(managerA, cross.id(), null));
        assertThat(jdbc.queryForObject("select user_id from shift_schedules where id = ?", UUID.class, mine)).isEqualTo(staff2);
        assertThat(jdbc.queryForObject("select user_id from shift_schedules where id = ?", UUID.class, theirs)).isEqualTo(staff1);

        ShiftDtos.Swap same = job(() -> swaps.create(staff1, new ShiftDtos.SwapCreate(mineSameDay, staff2, theirsSameDay, "Đổi giờ")));
        job(() -> swaps.accept(staff2, same.id(), null));
        job(() -> swaps.approve(managerA, same.id(), null));
        assertThat(policy.ruleFor(staff1, storeA, d2.plusDays(2)).start()).isEqualTo(LocalTime.of(14, 0));
        assertThat(policy.ruleFor(staff2, storeA, d2.plusDays(2)).start()).isEqualTo(LocalTime.of(7, 0));
    }

    @Test
    void giveAwayToColleagueAlreadyWorkingThatDayIsRejected() {
        LocalDate day = monday.plusDays(4);
        UUID mine = assign(staff1, day, morning).id();
        assign(staff2, day, afternoon);
        publishWeek();
        assertThat(swaps.colleagues(staff1, mine)).filteredOn(c -> c.id().equals(staff2)).singleElement()
                .satisfies(c -> assertThat(c.freeOnDate()).isFalse());
        assertThatThrownBy(() -> job(() -> swaps.create(staff1, new ShiftDtos.SwapCreate(mine, staff2, null, "Nhờ"))))
                .hasMessageContaining("đã có ca");
        assertThatThrownBy(() -> job(() -> swaps.create(staff1, new ShiftDtos.SwapCreate(mine, staffB, null, "Nhờ"))))
                .hasMessageContaining("cùng cửa hàng");
    }

    @Test
    void managerChangeCancelsOpenSwap() {
        LocalDate day = monday.plusDays(5);
        UUID mine = assign(staff1, day, morning).id();
        publishWeek();
        ShiftDtos.Swap swap = job(() -> swaps.create(staff1, new ShiftDtos.SwapCreate(mine, staff2, null, "Nhờ")));
        job(() -> schedules.update(managerA, mine, request(staff1, day, afternoon)));
        assertThat(swaps.get(swap.id()).status()).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("select status from shift_schedules where id = ?", String.class, mine)).isEqualTo("PUBLISHED");
    }

    @Test
    void actualStatusComparesScheduleWithAttendance() {
        LocalDate today = LocalDate.of(2026, 11, 6);
        LocalTime s = LocalTime.of(8, 0);
        LocalTime e = LocalTime.of(14, 0);
        LocalDateTime in = today.atTime(8, 20);
        assertThat(ShiftScheduleService.actualStatus(today.plusDays(1), s, e, null, null, false, 0, 0, false, today, LocalTime.NOON))
                .isEqualTo("UPCOMING");
        assertThat(ShiftScheduleService.actualStatus(today, s, e, null, null, false, 0, 0, false, today, LocalTime.of(7, 0)))
                .isEqualTo("UPCOMING");
        assertThat(ShiftScheduleService.actualStatus(today, s, e, null, null, false, 0, 0, false, today, LocalTime.of(9, 0)))
                .isEqualTo("NOT_CHECKED_IN");
        assertThat(ShiftScheduleService.actualStatus(today, s, e, null, null, false, 0, 0, false, today, LocalTime.of(15, 0)))
                .isEqualTo("ABSENT");
        assertThat(ShiftScheduleService.actualStatus(today.minusDays(1), s, e, null, null, false, 0, 0, false, today, LocalTime.NOON))
                .isEqualTo("ABSENT");
        assertThat(ShiftScheduleService.actualStatus(today.minusDays(1), s, e, null, null, false, 0, 0, true, today, LocalTime.NOON))
                .isEqualTo("ON_LEAVE");
        assertThat(ShiftScheduleService.actualStatus(today, s, e, in, null, true, 20, 0, false, today, LocalTime.NOON))
                .isEqualTo("WORKING");
        assertThat(ShiftScheduleService.actualStatus(today.minusDays(1), s, e, in.minusDays(1), null, true, 20, 0, false, today, LocalTime.NOON))
                .isEqualTo("MISSING_CHECK_OUT");
        assertThat(ShiftScheduleService.actualStatus(today, s, e, in, today.atTime(14, 0), true, 20, 0, false, today, LocalTime.of(15, 0)))
                .isEqualTo("LATE");
        assertThat(ShiftScheduleService.actualStatus(today, s, e, today.atTime(8, 0), today.atTime(14, 0), true, 0, 0, false, today,
                LocalTime.of(15, 0))).isEqualTo("ON_TIME");
    }

    @Test
    void pastShiftWithoutCheckInShowsAsAbsentInGrid() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        jdbc.update("""
                insert into shift_schedules(user_id, store_id, work_date, shift_id, shift_name, color, start_time, end_time, status, created_at)
                values (?, ?, ?, ?, 'Sáng', 'sky', '07:00', '13:00', 'PUBLISHED', now())
                """, staff1, storeA, yesterday, morning);
        ShiftDtos.Grid grid = schedules.grid(managerA, storeA, yesterday);
        assertThat(grid.entries()).filteredOn(e -> e.userId().equals(staff1)).singleElement().satisfies(e -> {
            assertThat(e.actualStatus()).isEqualTo("ABSENT");
            assertThat(e.editable()).isFalse();
        });
        assertThat(grid.stats().absent()).isEqualTo(1);
    }

    @Test
    void availabilityIsLimitedToUpcomingDays() {
        ShiftDtos.Availability a = job(() -> schedules.setAvailability(staff1,
                new ShiftDtos.AvailabilityRequest(monday, "available", morning, "Rảnh sáng")));
        assertThat(a.kind()).isEqualTo("AVAILABLE");
        assertThat(a.preferredShiftName()).isEqualTo("Sáng");
        job(() -> schedules.setAvailability(staff1, new ShiftDtos.AvailabilityRequest(monday, "UNAVAILABLE", morning, null)));
        assertThat(schedules.availability(staff1, monday, monday)).singleElement().satisfies(x -> {
            assertThat(x.kind()).isEqualTo("UNAVAILABLE");
            assertThat(x.preferredShiftId()).isNull();
        });
        assertThatThrownBy(() -> job(() -> schedules.setAvailability(staff1,
                new ShiftDtos.AvailabilityRequest(LocalDate.now().minusDays(1), "AVAILABLE", null, null)))).hasMessageContaining("đăng ký");
    }

    private void publishWeek() {
        job(() -> schedules.publish(managerA, new ShiftDtos.PublishRequest(storeA, monday, monday.plusDays(13))));
    }

    private ShiftDtos.Entry assign(UUID user, LocalDate day, UUID shift) {
        return job(() -> schedules.create(managerA, request(user, day, shift)));
    }

    private ShiftDtos.EntryRequest request(UUID user, LocalDate day, UUID shift) {
        return new ShiftDtos.EntryRequest(user, storeA, day, shift, null, null, null, null);
    }

    private void store(UUID id, String suffix, BigDecimal lat) {
        jdbc.update("insert into stores(id, code, name, active, latitude, longitude) values (?, ?, ?, true, ?, 106.700000)",
                id, "ZQS-" + suffix + tag, "Zqs Store " + suffix, lat);
    }

    private void user(UUID id, String name) {
        jdbc.update("insert into users(id, username, full_name, employee_code, active, employment_status) values (?, ?, ?, ?, true, 'ACTIVE')",
                id, "zqs-" + name + "-" + tag, "Zqs " + name, "ZQS" + name + tag);
    }

    private void staff(UUID user, UUID store) {
        jdbc.update("insert into store_staffs(user_id, store_id, active, start_date) values (?, ?, true, '2025-01-01')", user, store);
    }

    private <T> T job(Supplier<T> action) {
        try (var ignored = BusinessAuditContextFactory.systemJob("TEST_SHIFT")) {
            return action.get();
        }
    }
}
