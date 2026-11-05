package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.entity.LeaveRequest;
import com.fashionsystem.fashion_system.entity.LeaveType;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.LeaveRequestRepository;
import com.fashionsystem.fashion_system.repository.LeaveTypeRepository;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class LeaveRequestService {
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    @Value("${application.sequence.leave-code-prefix:LEV}")
    private String leaveCodePrefix;

    public LeaveRequestResponse createLeaveRequest(UUID userId, CreateLeaveRequest request) {
        if (request.startDate().isBefore(LocalDate.now(ZoneId.of("UTC")))) {
            throw BusinessException.badRequest("Ngày bắt đầu không thể trong quá khứ");
        }

        LeaveType leaveType = leaveTypeRepository.findById(request.leaveTypeId())
                .orElseThrow(() -> BusinessException.notFound("Loại nghỉ phép không tìm thấy"));

        List<LeaveRequest> overlapping = leaveRequestRepository.findOverlappingLeaves(
                userId, request.startDate(), request.endDate());

        if (!overlapping.isEmpty()) {
            throw BusinessException.conflict("Bạn đã có đơn xin nghỉ trùng ngày này");
        }

        String code = generateLeaveCode();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));

        LeaveRequest leave = LeaveRequest.builder()
                .code(code)
                .userId(userId)
                .leaveTypeId(request.leaveTypeId())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .isHalfDayStart(request.isHalfDayStart() != null && request.isHalfDayStart())
                .isHalfDayEnd(request.isHalfDayEnd() != null && request.isHalfDayEnd())
                .reason(request.reason())
                .status("PENDING")
                .createdAt(now)
                .build();

        leave = leaveRequestRepository.save(leave);
        return toResponse(leave, leaveType);
    }

    public LeaveRequestResponse approveLeaveRequest(
            UUID leaveId, ApproveLeaveRequest request, UUID approverId) {
        LeaveRequest leave = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> BusinessException.notFound("Đơn xin nghỉ không tìm thấy"));

        if (!"PENDING".equals(leave.getStatus())) {
            throw BusinessException.conflict("Chỉ có thể duyệt đơn ở trạng thái PENDING");
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        leave.setStatus("APPROVED");
        leave.setApprovedBy(approverId);
        leave.setApprovedAt(now);
        leave.setUpdatedAt(now);

        leave = leaveRequestRepository.save(leave);

        LeaveType leaveType = leaveTypeRepository.findById(leave.getLeaveTypeId())
                .orElse(null);

        return toResponse(leave, leaveType);
    }

    public LeaveRequestResponse rejectLeaveRequest(
            UUID leaveId, RejectLeaveRequest request, UUID approverId) {
        LeaveRequest leave = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> BusinessException.notFound("Đơn xin nghỉ không tìm thấy"));

        if (!"PENDING".equals(leave.getStatus())) {
            throw BusinessException.conflict("Chỉ có thể từ chối đơn ở trạng thái PENDING");
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        leave.setStatus("REJECTED");
        leave.setApprovedBy(approverId);
        leave.setApprovedAt(now);
        leave.setRejectionReason(request.reason());
        leave.setUpdatedAt(now);

        leave = leaveRequestRepository.save(leave);

        LeaveType leaveType = leaveTypeRepository.findById(leave.getLeaveTypeId())
                .orElse(null);

        return toResponse(leave, leaveType);
    }

    private String generateLeaveCode() {
        long timestamp = System.currentTimeMillis();
        return leaveCodePrefix + timestamp;
    }

    private LeaveRequestResponse toResponse(LeaveRequest leave, LeaveType leaveType) {
        return new LeaveRequestResponse(
                leave.getId(),
                leave.getCode(),
                leave.getUserId(),
                leave.getLeaveTypeId(),
                leaveType != null ? leaveType.getName() : "",
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getIsHalfDayStart(),
                leave.getIsHalfDayEnd(),
                leave.getStoreId(),
                leave.getReason(),
                leave.getStatus(),
                leave.getApprovedBy(),
                leave.getApprovedAt(),
                leave.getCreatedAt());
    }
}
