package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.entity.ExcuseRequest;
import com.fashionsystem.fashion_system.entity.AttendanceRecord;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.ExcuseRequestRepository;
import com.fashionsystem.fashion_system.repository.AttendanceRecordRepository;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ExcuseRequestService {
    private final ExcuseRequestRepository excuseRequestRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    @Value("${application.sequence.excuse-code-prefix:EXC}")
    private String excuseCodePrefix;

    public ExcuseRequestResponse createExcuse(UUID userId, CreateExcuseRequest request) {
        if (request.attendanceDate().isAfter(LocalDate.now(ZoneId.of("UTC")))) {
            throw BusinessException.badRequest("Không thể tạo giải trình cho ngày trong tương lai");
        }

        ExcuseRequest existing = excuseRequestRepository.findExisting(
                userId, request.attendanceDate(), request.excuseType()).orElse(null);

        if (existing != null && !"REJECTED".equals(existing.getStatus())) {
            throw BusinessException.conflict("Bạn đã tạo giải trình cho ngày này rồi");
        }

        String code = generateExcuseCode();

        ExcuseRequest excuse = ExcuseRequest.builder()
                .code(code)
                .userId(userId)
                .attendanceDate(request.attendanceDate())
                .excuseType(request.excuseType())
                .proposedCheckIn(request.proposedCheckIn())
                .proposedCheckOut(request.proposedCheckOut())
                .reason(request.reason())
                .status("PENDING")
                .createdAt(LocalDateTime.now(ZoneId.of("UTC")))
                .build();

        excuse = excuseRequestRepository.save(excuse);
        return toResponse(excuse);
    }

    public ExcuseRequestResponse approveExcuse(UUID excuseId, ApproveExcuseRequest request, UUID approverId) {
        ExcuseRequest excuse = excuseRequestRepository.findById(excuseId)
                .orElseThrow(() -> BusinessException.notFound("Đơn giải trình không tìm thấy"));

        if (!"PENDING".equals(excuse.getStatus())) {
            throw BusinessException.conflict("Chỉ có thể duyệt đơn ở trạng thái PENDING");
        }

        AttendanceRecord record = attendanceRecordRepository.findByUserIdAndAttendanceDate(
                excuse.getUserId(), excuse.getAttendanceDate()).orElse(null);

        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        excuse.setStatus("APPROVED");
        excuse.setApprovedBy(approverId);
        excuse.setApprovedAt(now);
        excuse.setUpdatedAt(now);

        if (record == null) {
            record = AttendanceRecord.builder()
                    .userId(excuse.getUserId())
                    .attendanceDate(excuse.getAttendanceDate())
                    .status("PRESENT")
                    .createdAt(now)
                    .build();
        }

        if ("FORGOT_CHECK_IN".equals(excuse.getExcuseType()) && request.proposedCheckIn() != null) {
            record.setCheckInTime(request.proposedCheckIn());
        }
        if ("FORGOT_CHECK_OUT".equals(excuse.getExcuseType()) && request.proposedCheckOut() != null) {
            record.setCheckOutTime(request.proposedCheckOut());
        }

        record.setUpdatedAt(now);
        attendanceRecordRepository.save(record);
        excuse = excuseRequestRepository.save(excuse);

        return toResponse(excuse);
    }

    public ExcuseRequestResponse rejectExcuse(UUID excuseId, RejectExcuseRequest request, UUID approverId) {
        ExcuseRequest excuse = excuseRequestRepository.findById(excuseId)
                .orElseThrow(() -> BusinessException.notFound("Đơn giải trình không tìm thấy"));

        if (!"PENDING".equals(excuse.getStatus())) {
            throw BusinessException.conflict("Chỉ có thể từ chối đơn ở trạng thái PENDING");
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        excuse.setStatus("REJECTED");
        excuse.setApprovedBy(approverId);
        excuse.setApprovedAt(now);
        excuse.setRejectionReason(request.reason());
        excuse.setUpdatedAt(now);

        excuse = excuseRequestRepository.save(excuse);
        return toResponse(excuse);
    }

    private String generateExcuseCode() {
        long timestamp = System.currentTimeMillis();
        return excuseCodePrefix + timestamp;
    }

    private ExcuseRequestResponse toResponse(ExcuseRequest excuse) {
        return new ExcuseRequestResponse(
                excuse.getId(),
                excuse.getCode(),
                excuse.getUserId(),
                excuse.getAttendanceDate(),
                excuse.getExcuseType(),
                excuse.getProposedCheckIn(),
                excuse.getProposedCheckOut(),
                excuse.getReason(),
                excuse.getStatus(),
                excuse.getApprovedBy(),
                excuse.getApprovedAt(),
                excuse.getCreatedAt());
    }
}
