package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.entity.AttendanceRecord;
import com.fashionsystem.fashion_system.entity.AttendanceAuditLog;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.entity.StoreStaff;
import com.fashionsystem.fashion_system.entity.EmployeeShift;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.AttendanceRecordRepository;
import com.fashionsystem.fashion_system.repository.AttendanceAuditLogRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.repository.StoreStaffRepository;
import com.fashionsystem.fashion_system.repository.EmployeeShiftRepository;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AttendanceRecordService {
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final EmployeeShiftRepository employeeShiftRepository;
    private final ObjectMapper objectMapper;

    private static final double LOCATION_TOLERANCE_KM = 0.5;

    public AttendanceResponse checkIn(UUID userId, CheckInRequest request) {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("Nhân viên không tìm thấy"));

        AttendanceRecord existing = attendanceRecordRepository.findByUserIdAndAttendanceDate(userId, today)
                .orElse(null);

        if (existing != null && existing.getCheckInTime() != null) {
            throw BusinessException.conflict("Bạn đã check-in hôm nay rồi");
        }

        Optional<EmployeeShift> shiftAssignment = employeeShiftRepository.findActiveOnDate(userId, today);

        AttendanceRecord record = existing != null ? existing : AttendanceRecord.builder()
                .userId(userId)
                .attendanceDate(today)
                .build();

        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        record.setCheckInTime(now);
        record.setCheckInLocationLat(request.latitude());
        record.setCheckInLocationLon(request.longitude());

        if (shiftAssignment.isPresent()) {
            record.setShiftId(shiftAssignment.get().getShiftId());
        }

        if (request.storeId() != null) {
            validateStoreAccess(userId, request.storeId());
            record.setStoreId(request.storeId());
        } else {
            Optional<Store> defaultStore = resolveDefaultStore(userId);
            if (defaultStore.isPresent()) {
                record.setStoreId(defaultStore.get().getId());
            }
        }

        record.setStatus("PRESENT");
        record.setCreatedAt(now);
        record = attendanceRecordRepository.save(record);

        logAuditAction(record.getId(), "CHECK_IN", null, toJsonNode(record), "Check-in", userId);

        return toResponse(record);
    }

    public AttendanceResponse checkOut(UUID userId, CheckOutRequest request) {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));

        AttendanceRecord record = attendanceRecordRepository.findByUserIdAndAttendanceDate(userId, today)
                .orElseThrow(() -> BusinessException.notFound("Chưa check-in hôm nay"));

        if (record.getCheckInTime() == null) {
            throw BusinessException.conflict("Bạn phải check-in trước khi check-out");
        }

        if (record.getCheckOutTime() != null) {
            throw BusinessException.conflict("Bạn đã check-out rồi");
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));

        ObjectNode oldData = toJsonNode(record);
        record.setCheckOutTime(now);
        record.setCheckOutLocationLat(request.latitude());
        record.setCheckOutLocationLon(request.longitude());
        record.setUpdatedAt(now);
        record = attendanceRecordRepository.save(record);

        logAuditAction(record.getId(), "CHECK_OUT", oldData, toJsonNode(record), "Check-out", userId);

        return toResponse(record);
    }

    public TodayAttendanceResponse getTodayStatus(UUID userId) {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));

        AttendanceRecord record = attendanceRecordRepository.findByUserIdAndAttendanceDate(userId, today)
                .orElse(null);

        if (record == null) {
            return new TodayAttendanceResponse(
                    null, false, false, null, null, false, null, "NOT_CHECKED_IN", "");
        }

        boolean hasCheckIn = record.getCheckInTime() != null;
        boolean hasCheckOut = record.getCheckOutTime() != null;
        Long durationMinutes = null;

        if (hasCheckIn && hasCheckOut) {
            durationMinutes = java.time.temporal.ChronoUnit.MINUTES.between(
                    record.getCheckInTime(), record.getCheckOutTime());
        }

        boolean locationCaptured = record.getCheckInLocationLat() != null;

        return new TodayAttendanceResponse(
                record.getId(),
                hasCheckIn,
                hasCheckOut,
                record.getCheckInTime(),
                record.getCheckOutTime(),
                locationCaptured,
                durationMinutes,
                record.getStatus(),
                "");
    }

    public AttendanceHistoryResponse getHistory(UUID userId, AttendanceHistoryRequest request) {
        List<AttendanceRecord> records = attendanceRecordRepository.findByUserAndDateRange(
                userId, request.fromDate(), request.toDate());

        List<AttendanceResponse> responses = records.stream()
                .map(this::toResponse)
                .toList();

        return new AttendanceHistoryResponse(responses, responses.size());
    }

    @Transactional(readOnly = true)
    public AttendanceRecordsListResponse searchRecords(
            UUID storeId, LocalDate fromDate, LocalDate toDate, String status, int page, int size) {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size);

        var pageResult = attendanceRecordRepository.searchRecords(storeId, fromDate, toDate, status, pageable);

        List<AttendanceRecordDetailResponse> details = pageResult.getContent().stream()
                .map(this::toDetailResponse)
                .toList();

        return new AttendanceRecordsListResponse(
                details,
                (int) pageResult.getTotalElements(),
                page,
                size);
    }

    public AttendanceResponse adjustAttendance(
            UUID recordId, AdjustAttendanceRequest request, UUID actorId) {
        AttendanceRecord record = attendanceRecordRepository.findById(recordId)
                .orElseThrow(() -> BusinessException.notFound("Bản ghi chấm công không tìm thấy"));

        ObjectNode oldData = toJsonNode(record);

        if (request.newCheckIn() != null) {
            record.setCheckInTime(request.newCheckIn());
        }
        if (request.newCheckOut() != null) {
            record.setCheckOutTime(request.newCheckOut());
        }

        record.setUpdatedAt(LocalDateTime.now(ZoneId.of("UTC")));
        record = attendanceRecordRepository.save(record);

        logAuditAction(record.getId(), "ADJUST", oldData, toJsonNode(record), request.reason(), actorId);

        return toResponse(record);
    }

    private void validateStoreAccess(UUID userId, UUID storeId) {
        StoreStaff staff = storeStaffRepository.findByUserIdAndStoreId(userId, storeId)
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .orElse(null);

        if (staff == null) {
            throw BusinessException.forbidden("Bạn không được phân công tại cửa hàng này");
        }
    }

    private Optional<Store> resolveDefaultStore(UUID userId) {
        return storeStaffRepository.findByUserId(userId).stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .findFirst()
                .flatMap(staff -> storeRepository.findById(staff.getStoreId()));
    }

    private void logAuditAction(UUID recordId, String action, ObjectNode oldData,
                                ObjectNode newData, String reason, UUID actorId) {
        AttendanceAuditLog log = AttendanceAuditLog.builder()
                .attendanceRecordId(recordId)
                .action(action)
                .oldData(oldData)
                .newData(newData)
                .reason(reason)
                .actorId(actorId)
                .createdAt(LocalDateTime.now(ZoneId.of("UTC")))
                .build();
        auditLogRepository.save(log);
    }

    private ObjectNode toJsonNode(AttendanceRecord record) {
        return objectMapper.valueToTree(record);
    }

    private AttendanceResponse toResponse(AttendanceRecord record) {
        return new AttendanceResponse(
                record.getId(),
                record.getUserId(),
                record.getAttendanceDate(),
                record.getStoreId(),
                record.getShiftId(),
                record.getCheckInTime(),
                record.getCheckOutTime(),
                record.getCheckInLocationLat(),
                record.getCheckInLocationLon(),
                record.getCheckOutLocationLat(),
                record.getCheckOutLocationLon(),
                record.getStatus(),
                record.getNotes(),
                record.getCreatedAt());
    }

    private AttendanceRecordDetailResponse toDetailResponse(AttendanceRecord record) {
        User user = userRepository.findById(record.getUserId()).orElse(null);
        Store store = record.getStoreId() != null ? storeRepository.findById(record.getStoreId()).orElse(null) : null;

        long durationMinutes = 0;
        boolean isLate = false;
        boolean isEarlyDeparture = false;

        if (record.getCheckInTime() != null && record.getCheckOutTime() != null) {
            durationMinutes = java.time.temporal.ChronoUnit.MINUTES.between(
                    record.getCheckInTime(), record.getCheckOutTime());
        }

        return new AttendanceRecordDetailResponse(
                record.getId(),
                record.getUserId(),
                user != null ? user.getEmployeeCode() : "",
                user != null ? user.getFullName() : "",
                record.getStoreId(),
                store != null ? store.getName() : "",
                record.getAttendanceDate(),
                "",
                record.getCheckInTime(),
                record.getCheckOutTime(),
                durationMinutes,
                record.getStatus(),
                isLate,
                isEarlyDeparture);
    }
}
