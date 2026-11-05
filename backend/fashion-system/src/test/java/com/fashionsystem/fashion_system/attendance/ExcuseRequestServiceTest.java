package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.entity.ExcuseRequest;
import com.fashionsystem.fashion_system.entity.AttendanceRecord;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.ExcuseRequestRepository;
import com.fashionsystem.fashion_system.repository.AttendanceRecordRepository;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ExcuseRequestServiceTest {

    private ExcuseRequestService service;

    @Mock
    private ExcuseRequestRepository excuseRequestRepository;
    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    private UUID userId;
    private LocalDate yesterday;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ExcuseRequestService(excuseRequestRepository, attendanceRecordRepository);
        userId = UUID.randomUUID();
        yesterday = LocalDate.now(ZoneId.of("UTC")).minusDays(1);
    }

    @Test
    void testCreateExcuse_Success() {
        CreateExcuseRequest request = new CreateExcuseRequest(
                yesterday,
                "FORGOT_CHECK_IN",
                LocalDateTime.now(ZoneId.of("UTC")).minusHours(8),
                null,
                "System crashed"
        );

        when(excuseRequestRepository.findExisting(userId, yesterday, "FORGOT_CHECK_IN"))
                .thenReturn(Optional.empty());
        when(excuseRequestRepository.save(any())).thenAnswer(invocation -> {
            ExcuseRequest excuse = invocation.getArgument(0);
            excuse.setId(UUID.randomUUID());
            return excuse;
        });

        ExcuseRequestResponse response = service.createExcuse(userId, request);

        assertNotNull(response);
        assertEquals("PENDING", response.status());
        verify(excuseRequestRepository, times(1)).save(any());
    }

    @Test
    void testCreateExcuse_FutureDate() {
        LocalDate future = LocalDate.now(ZoneId.of("UTC")).plusDays(1);
        CreateExcuseRequest request = new CreateExcuseRequest(
                future,
                "FORGOT_CHECK_IN",
                null,
                null,
                "System crashed"
        );

        assertThrows(BusinessException.class, () -> service.createExcuse(userId, request));
    }

    @Test
    void testCreateExcuse_AlreadyExists() {
        CreateExcuseRequest request = new CreateExcuseRequest(
                yesterday,
                "FORGOT_CHECK_IN",
                null,
                null,
                "System crashed"
        );

        ExcuseRequest existing = ExcuseRequest.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .attendanceDate(yesterday)
                .excuseType("FORGOT_CHECK_IN")
                .status("PENDING")
                .build();

        when(excuseRequestRepository.findExisting(userId, yesterday, "FORGOT_CHECK_IN"))
                .thenReturn(Optional.of(existing));

        assertThrows(BusinessException.class, () -> service.createExcuse(userId, request));
    }

    @Test
    void testApproveExcuse_Success() {
        UUID excuseId = UUID.randomUUID();
        ApproveExcuseRequest request = new ApproveExcuseRequest(
                LocalDateTime.now(ZoneId.of("UTC")).minusHours(8),
                null
        );

        ExcuseRequest excuse = ExcuseRequest.builder()
                .id(excuseId)
                .userId(userId)
                .attendanceDate(yesterday)
                .excuseType("FORGOT_CHECK_IN")
                .status("PENDING")
                .proposedCheckIn(LocalDateTime.now(ZoneId.of("UTC")).minusHours(8))
                .build();

        when(excuseRequestRepository.findById(excuseId)).thenReturn(Optional.of(excuse));
        when(attendanceRecordRepository.findByUserIdAndAttendanceDate(userId, yesterday))
                .thenReturn(Optional.empty());
        when(attendanceRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(excuseRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExcuseRequestResponse response = service.approveExcuse(excuseId, request, UUID.randomUUID());

        assertNotNull(response);
        assertEquals("APPROVED", response.status());
        verify(attendanceRecordRepository, times(1)).save(any());
        verify(excuseRequestRepository, times(1)).save(any());
    }

    @Test
    void testRejectExcuse_Success() {
        UUID excuseId = UUID.randomUUID();
        RejectExcuseRequest request = new RejectExcuseRequest("Invalid reason");

        ExcuseRequest excuse = ExcuseRequest.builder()
                .id(excuseId)
                .userId(userId)
                .attendanceDate(yesterday)
                .excuseType("FORGOT_CHECK_IN")
                .status("PENDING")
                .build();

        when(excuseRequestRepository.findById(excuseId)).thenReturn(Optional.of(excuse));
        when(excuseRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExcuseRequestResponse response = service.rejectExcuse(excuseId, request, UUID.randomUUID());

        assertNotNull(response);
        assertEquals("REJECTED", response.status());
        verify(excuseRequestRepository, times(1)).save(any());
    }

    @Test
    void testApproveExcuse_NotPending() {
        UUID excuseId = UUID.randomUUID();
        ApproveExcuseRequest request = new ApproveExcuseRequest(null, null);

        ExcuseRequest excuse = ExcuseRequest.builder()
                .id(excuseId)
                .userId(userId)
                .attendanceDate(yesterday)
                .excuseType("FORGOT_CHECK_IN")
                .status("APPROVED")
                .build();

        when(excuseRequestRepository.findById(excuseId)).thenReturn(Optional.of(excuse));

        assertThrows(BusinessException.class, () -> service.approveExcuse(excuseId, request, UUID.randomUUID()));
    }
}
