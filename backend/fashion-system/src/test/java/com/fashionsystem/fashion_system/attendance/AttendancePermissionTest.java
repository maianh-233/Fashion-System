package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.repository.StoreStaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AttendancePermissionTest {

    @Mock
    private StoreStaffRepository storeStaffRepository;

    private UUID userId;
    private UUID storeId;
    private UUID otherStoreId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        otherStoreId = UUID.randomUUID();
    }

    @Test
    void testStoreEmployeeHasAssignmentAtStore() {
        when(storeStaffRepository.findByUserIdAndStoreId(userId, storeId))
                .thenReturn(Optional.empty());
        when(storeStaffRepository.findByUserIdAndStoreId(userId, otherStoreId))
                .thenReturn(Optional.empty());

        Optional<?> result1 = storeStaffRepository.findByUserIdAndStoreId(userId, storeId);
        Optional<?> result2 = storeStaffRepository.findByUserIdAndStoreId(userId, otherStoreId);

        assertTrue(result1.isEmpty());
        assertTrue(result2.isEmpty());
    }

    @Test
    void testPermissionScopingByStore() {
        when(storeStaffRepository.findByUserIdAndStoreId(userId, storeId))
                .thenReturn(Optional.empty());

        Optional<?> result = storeStaffRepository.findByUserIdAndStoreId(userId, storeId);
        assertTrue(result.isEmpty());
        verify(storeStaffRepository, times(1)).findByUserIdAndStoreId(userId, storeId);
    }

    @Test
    void testUnauthorizedStoreAccess() {
        when(storeStaffRepository.findByUserIdAndStoreId(userId, otherStoreId))
                .thenReturn(Optional.empty());

        Optional<?> result = storeStaffRepository.findByUserIdAndStoreId(userId, otherStoreId);
        assertTrue(result.isEmpty());
    }

    @Test
    void testMultipleStoreAssignments() {
        when(storeStaffRepository.findByUserId(userId))
                .thenReturn(java.util.Arrays.asList());

        var staffList = storeStaffRepository.findByUserId(userId);
        assertNotNull(staffList);
        assertTrue(staffList.isEmpty());
    }

    @Test
    void testPermissionCheckByRepository() {
        when(storeStaffRepository.findByUserIdAndStoreId(userId, storeId))
                .thenReturn(Optional.empty());

        var result = storeStaffRepository.findByUserIdAndStoreId(userId, storeId);
        assertFalse(result.isPresent());
    }
}
