package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.entity.AttendanceRecord;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.entity.StoreStaff;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.AttendanceRecordRepository;
import com.fashionsystem.fashion_system.repository.AttendanceAuditLogRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.repository.StoreStaffRepository;
import com.fashionsystem.fashion_system.repository.EmployeeShiftRepository;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AttendanceRecordServiceTest {

    private AttendanceRecordService service;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;
    @Mock
    private AttendanceAuditLogRepository auditLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreStaffRepository storeStaffRepository;
    @Mock
    private EmployeeShiftRepository employeeShiftRepository;
    @Mock
    private ObjectMapper objectMapper;

    private UUID userId;
    private UUID storeId;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AttendanceRecordService(
                attendanceRecordRepository,
                auditLogRepository,
                userRepository,
                storeRepository,
                storeStaffRepository,
                employeeShiftRepository,
                objectMapper
        );

        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        today = LocalDate.now(ZoneId.of("UTC"));
    }

    @Test
    void testCheckIn_Success() {
        User mockUser = User.builder().id(userId).fullName("Test Employee").build();
        CheckInRequest request = new CheckInRequest(storeId, BigDecimal.valueOf(10.123), BigDecimal.valueOf(20.456));

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(attendanceRecordRepository.findByUserIdAndAttendanceDate(userId, today)).thenReturn(Optional.empty());
        when(employeeShiftRepository.findActiveOnDate(userId, today)).thenReturn(Optional.empty());
        when(storeStaffRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(
                StoreStaff.builder().userId(userId).storeId(storeId).active(true).build()
        ));
        when(attendanceRecordRepository.save(any())).thenAnswer(invocation -> {
            AttendanceRecord record = invocation.getArgument(0);
            record.setId(UUID.randomUUID());
            return record;
        });
        when(objectMapper.valueToTree(any())).thenReturn(null);

        AttendanceResponse response = service.checkIn(userId, request);

        assertNotNull(response);
        verify(attendanceRecordRepository, times(1)).save(any());
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    void testCheckIn_UserNotFound() {
        CheckInRequest request = new CheckInRequest(storeId, BigDecimal.valueOf(10.123), BigDecimal.valueOf(20.456));

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.checkIn(userId, request));
    }

    @Test
    void testCheckIn_AlreadyCheckedIn() {
        User mockUser = User.builder().id(userId).fullName("Test Employee").build();
        CheckInRequest request = new CheckInRequest(storeId, BigDecimal.valueOf(10.123), BigDecimal.valueOf(20.456));

        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        AttendanceRecord existing = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .attendanceDate(today)
                .checkInTime(now)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(attendanceRecordRepository.findByUserIdAndAttendanceDate(userId, today)).thenReturn(Optional.of(existing));

        assertThrows(BusinessException.class, () -> service.checkIn(userId, request));
    }

    @Test
    void testCheckOut_Success() {
        CheckOutRequest request = new CheckOutRequest(BigDecimal.valueOf(10.123), BigDecimal.valueOf(20.456));

        LocalDateTime checkInTime = LocalDateTime.now(ZoneId.of("UTC")).minusHours(8);
        AttendanceRecord existing = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .attendanceDate(today)
                .checkInTime(checkInTime)
                .build();

        when(attendanceRecordRepository.findByUserIdAndAttendanceDate(userId, today))
                .thenReturn(Optional.of(existing));
        when(attendanceRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.valueToTree(any())).thenReturn(null);

        AttendanceResponse response = service.checkOut(userId, request);

        assertNotNull(response);
        verify(attendanceRecordRepository, times(1)).save(any());
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    void testCheckOut_NoCheckInRecord() {
        CheckOutRequest request = new CheckOutRequest(BigDecimal.valueOf(10.123), BigDecimal.valueOf(20.456));

        when(attendanceRecordRepository.findByUserIdAndAttendanceDate(userId, today)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.checkOut(userId, request));
    }

    @Test
    void testCheckOut_NoCheckInTime() {
        CheckOutRequest request = new CheckOutRequest(BigDecimal.valueOf(10.123), BigDecimal.valueOf(20.456));

        AttendanceRecord existing = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .attendanceDate(today)
                .checkInTime(null)
                .build();

        when(attendanceRecordRepository.findByUserIdAndAttendanceDate(userId, today))
                .thenReturn(Optional.of(existing));

        assertThrows(BusinessException.class, () -> service.checkOut(userId, request));
    }

    @Test
    void testGetTodayStatus_HasCheckInAndOut() {
        LocalDateTime checkInTime = LocalDateTime.now(ZoneId.of("UTC")).minusHours(8);
        LocalDateTime checkOutTime = LocalDateTime.now(ZoneId.of("UTC"));

        AttendanceRecord record = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .attendanceDate(today)
                .checkInTime(checkInTime)
                .checkOutTime(checkOutTime)
                .status("PRESENT")
                .checkInLocationLat(BigDecimal.valueOf(10.123))
                .build();

        when(attendanceRecordRepository.findByUserIdAndAttendanceDate(userId, today))
                .thenReturn(Optional.of(record));

        TodayAttendanceResponse response = service.getTodayStatus(userId);

        assertNotNull(response);
        assertEquals("PRESENT", response.status());
    }

    @Test
    void testGetTodayStatus_NoRecord() {
        when(attendanceRecordRepository.findByUserIdAndAttendanceDate(userId, today))
                .thenReturn(Optional.empty());

        TodayAttendanceResponse response = service.getTodayStatus(userId);

        assertNotNull(response);
        assertEquals("NOT_CHECKED_IN", response.status());
    }
}
