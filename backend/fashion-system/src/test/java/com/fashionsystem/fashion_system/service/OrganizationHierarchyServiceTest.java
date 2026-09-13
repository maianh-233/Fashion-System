package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.Position;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.exception.HierarchyConfirmationRequiredException;
import com.fashionsystem.fashion_system.service.OrganizationHierarchyService.HierarchyImpact;
import com.fashionsystem.fashion_system.repository.PositionRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrganizationHierarchyServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PositionRepository positions = mock(PositionRepository.class);
    private final Map<UUID, User> employees = new HashMap<>();
    private final Map<UUID, Position> jobs = new HashMap<>();
    private final UUID department = UUID.randomUUID();
    private OrganizationHierarchyService service;
    private User manager;
    private User subordinate;

    @BeforeEach
    void setUp() {
        when(users.findById(any())).thenAnswer(call -> Optional.ofNullable(employees.get(call.getArgument(0))));
        when(positions.findById(any())).thenAnswer(call -> Optional.ofNullable(jobs.get(call.getArgument(0))));
        service = new OrganizationHierarchyService(users, positions);
        manager = employee(position(department, 3));
        subordinate = employee(position(department, 1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"manager", "subordinate"})
    void rejectsMissingEmployee(String side) {
        assertThrows(BusinessException.class, () -> service.validateAssignment(
                side.equals("manager") ? null : manager, side.equals("subordinate") ? null : subordinate));
    }

    @ParameterizedTest
    @ValueSource(strings = {"manager", "subordinate"})
    void rejectsDeletedEmployee(String side) {
        (side.equals("manager") ? manager : subordinate).setDeletedAt(LocalDateTime.now());
        assertThrows(BusinessException.class, () -> service.validateAssignment(manager, subordinate));
    }

    @ParameterizedTest
    @ValueSource(strings = {"manager", "subordinate"})
    void rejectsMissingPositionId(String side) {
        (side.equals("manager") ? manager : subordinate).setPositionId(null);
        assertThrows(BusinessException.class, () -> service.validateAssignment(manager, subordinate));
    }

    @ParameterizedTest
    @ValueSource(strings = {"manager", "subordinate"})
    void rejectsMissingPositionRecord(String side) {
        jobs.remove((side.equals("manager") ? manager : subordinate).getPositionId());
        assertThrows(BusinessException.class, () -> service.validateAssignment(manager, subordinate));
    }

    @Test
    void rejectsDifferentPositionDepartments() {
        jobs.get(subordinate.getPositionId()).setDepartmentId(UUID.randomUUID());
        assertThrows(BusinessException.class, () -> service.validateAssignment(manager, subordinate));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    void rejectsEqualOrLowerManagerLevel(int level) {
        jobs.get(subordinate.getPositionId()).setHierarchyLevel(3);
        jobs.get(manager.getPositionId()).setHierarchyLevel(level);
        assertThrows(BusinessException.class, () -> service.validateAssignment(manager, subordinate));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PART_TIME", "INTERN", ""})
    void rejectsNonFullTimeManager(String type) {
        manager.setEmploymentType(type);
        assertThrows(BusinessException.class, () -> service.validateAssignment(manager, subordinate));
    }

    @Test
    void allowsHigherFullTimeManagerAndPartTimeSubordinate() {
        subordinate.setEmploymentType("PART_TIME");
        assertDoesNotThrow(() -> service.validateAssignment(manager, subordinate));
    }

    @Test
    void structuralRulesDoNotExcludeInactiveOrLockedEmployees() {
        manager.setActive(false);
        subordinate.setActive(false);
        subordinate.setLocked(true);
        assertDoesNotThrow(() -> service.validateAssignment(manager, subordinate));
    }

    @Test
    void rejectsSelfAssignment() {
        assertThrows(BusinessException.class, () -> service.validateAssignment(manager, manager));
    }

    @Test
    void rejectsCycleThroughManagerAncestors() {
        User ancestor = employee(position(department, 4));
        manager.setManagerId(ancestor.getId());
        ancestor.setManagerId(subordinate.getId());
        assertThrows(BusinessException.class, () -> service.validateAssignment(manager, subordinate));
    }

    @Test
    void rejectsExistingAncestorCycleWithoutLoopingForever() {
        User first = employee(position(department, 4));
        User second = employee(position(department, 5));
        manager.setManagerId(first.getId());
        first.setManagerId(second.getId());
        second.setManagerId(first.getId());
        assertThrows(BusinessException.class, () -> service.validateAssignment(manager, subordinate));
    }

    @Test
    void demotionInvalidatesOnlySubordinatesAtOrAboveProposedLevelWithoutMutation() {
        subordinate.setManagerId(manager.getId());
        User peer = employee(position(department, 2));
        peer.setManagerId(manager.getId());
        incidentQueries();
        Position proposed = position(department, 2);
        UUID originalPositionId = manager.getPositionId();

        HierarchyImpact impact = service.analyzeEmployeeChange(manager, proposed);

        assertEquals(Set.of(peer.getId()), impact.invalidSubordinateIds());
        assertEquals(Set.of(manager.getId(), peer.getId()), impact.affectedEmployeeIds());
        assertEquals(originalPositionId, manager.getPositionId());
        assertEquals(manager.getId(), peer.getManagerId());
        assertEquals(manager.getId(), subordinate.getManagerId());
        verify(users, never()).save(any());
    }

    @Test
    void employeeDepartmentMoveIncludesIncomingAndOutgoingLinksAndDistinctPeople() {
        User supervisor = employee(position(department, 4));
        manager.setManagerId(supervisor.getId());
        subordinate.setManagerId(manager.getId());
        incidentQueries();

        HierarchyImpact impact = service.analyzeEmployeeChange(manager, position(UUID.randomUUID(), 3));

        assertEquals(Set.of(manager.getId(), subordinate.getId()), impact.invalidSubordinateIds());
        assertEquals(Set.of(supervisor.getId(), manager.getId(), subordinate.getId()), impact.affectedEmployeeIds());
        assertEquals(2L, impact.toResponse().affectedRelationCount());
        assertEquals(3L, impact.toResponse().affectedEmployeeCount());
        assertFalse(impact.toResponse().summary().isBlank());
    }

    @Test
    void employeePromotionCanInvalidateItsOwnManagerLink() {
        subordinate.setManagerId(manager.getId());
        incidentQueries();

        HierarchyImpact impact = service.analyzeEmployeeChange(subordinate, position(department, 3));

        assertEquals(Set.of(subordinate.getId()), impact.invalidSubordinateIds());
        assertEquals(Set.of(manager.getId(), subordinate.getId()), impact.affectedEmployeeIds());
    }

    @Test
    void removingEmployeePositionInvalidatesEveryIncidentLink() {
        subordinate.setManagerId(manager.getId());
        incidentQueries();

        assertEquals(Set.of(subordinate.getId()), service.analyzeEmployeeChange(manager, null).invalidSubordinateIds());
    }

    @Test
    void legacyInactiveAndLockedEmployeesAreIncludedInImpact() {
        manager.setActive(false);
        subordinate.setActive(false);
        subordinate.setLocked(true);
        subordinate.setManagerId(manager.getId());
        incidentQueries();

        assertEquals(Set.of(subordinate.getId()), service.analyzeEmployeeChange(manager,
                position(department, 1)).invalidSubordinateIds());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "deleted"})
    void invalidExistingManagerIsIncludedInImpact(String state) {
        subordinate.setManagerId(manager.getId());
        if (state.equals("missing")) employees.remove(manager.getId());
        else manager.setDeletedAt(LocalDateTime.now());
        incidentQueries();

        HierarchyImpact impact = service.analyzeEmployeeChange(subordinate, position(department, 2));

        assertEquals(Set.of(subordinate.getId()), impact.invalidSubordinateIds());
        assertEquals(Set.of(manager.getId(), subordinate.getId()), impact.affectedEmployeeIds());
    }

    @Test
    void deletedSubordinatesDoNotContributeToImpact() {
        subordinate.setManagerId(manager.getId());
        subordinate.setDeletedAt(LocalDateTime.now());
        incidentQueries();

        assertTrue(service.analyzeEmployeeChange(manager, position(department, 1)).isEmpty());
    }

    @Test
    void positionDemotionEvaluatesEveryHolderAsManager() {
        User otherManager = employee(jobs.get(manager.getPositionId()));
        User otherSubordinate = employee(jobs.get(subordinate.getPositionId()));
        subordinate.setManagerId(manager.getId());
        otherSubordinate.setManagerId(otherManager.getId());
        incidentQueries();
        Position position = jobs.get(manager.getPositionId());

        HierarchyImpact impact = service.analyzePositionChange(position, department, 1);

        assertEquals(Set.of(subordinate.getId(), otherSubordinate.getId()), impact.invalidSubordinateIds());
        assertEquals(4L, impact.toResponse().affectedEmployeeCount());
        assertEquals(3, position.getHierarchyLevel());
        assertEquals(department, position.getDepartmentId());
        verify(users, never()).save(any());
    }

    @Test
    void positionPromotionEvaluatesEveryHolderAsSubordinateAndDeduplicatesManager() {
        User otherSubordinate = employee(jobs.get(subordinate.getPositionId()));
        subordinate.setManagerId(manager.getId());
        otherSubordinate.setManagerId(manager.getId());
        incidentQueries();

        HierarchyImpact impact = service.analyzePositionChange(jobs.get(subordinate.getPositionId()), department, 3);

        assertEquals(Set.of(subordinate.getId(), otherSubordinate.getId()), impact.invalidSubordinateIds());
        assertEquals(2L, impact.toResponse().affectedRelationCount());
        assertEquals(3L, impact.toResponse().affectedEmployeeCount());
    }

    @Test
    void positionDepartmentMoveDeduplicatesLinkSeenFromBothHolders() {
        subordinate.setPositionId(manager.getPositionId());
        subordinate.setManagerId(manager.getId());
        incidentQueries();

        HierarchyImpact impact = service.analyzePositionChange(jobs.get(manager.getPositionId()), UUID.randomUUID(), 4);

        assertEquals(Set.of(subordinate.getId()), impact.invalidSubordinateIds());
        assertEquals(1L, impact.toResponse().affectedRelationCount());
        assertEquals(2L, impact.toResponse().affectedEmployeeCount());
    }

    @Test
    void positionDepartmentMoveEvaluatesBothIncomingAndOutgoingRelations() {
        User supervisor = employee(position(department, 4));
        manager.setManagerId(supervisor.getId());
        subordinate.setManagerId(manager.getId());
        incidentQueries();

        HierarchyImpact impact = service.analyzePositionChange(jobs.get(manager.getPositionId()), UUID.randomUUID(), 3);

        assertEquals(Set.of(manager.getId(), subordinate.getId()), impact.invalidSubordinateIds());
        assertEquals(3L, impact.toResponse().affectedEmployeeCount());
    }

    @Test
    void noOpChangesDoNotRequireConfirmationEvenWithLegacyInvalidLinks() {
        subordinate.setPositionId(manager.getPositionId());
        subordinate.setManagerId(manager.getId());
        incidentQueries();
        Position position = jobs.get(manager.getPositionId());

        HierarchyImpact employeeImpact = service.analyzeEmployeeChange(manager, position);
        HierarchyImpact positionImpact = service.analyzePositionChange(position, department, 3);

        assertTrue(employeeImpact.isEmpty());
        assertTrue(positionImpact.isEmpty());
        assertDoesNotThrow(() -> service.confirmOrClear(employeeImpact, null));
        assertDoesNotThrow(() -> service.confirmOrClear(positionImpact, false));
        verify(users, never()).save(any());
    }

    @Test
    void confirmationRequiredExposesStructured409AndDoesNotChangeRelations() {
        subordinate.setManagerId(manager.getId());
        HierarchyImpact impact = new HierarchyImpact(Set.of(subordinate.getId()), Set.of(manager.getId(), subordinate.getId()));

        for (Boolean confirmation : new Boolean[] {null, false}) {
            HierarchyConfirmationRequiredException error = assertThrows(HierarchyConfirmationRequiredException.class,
                    () -> service.confirmOrClear(impact, confirmation));
            assertEquals(409, error.getStatusCode().value());
            assertEquals("HIERARCHY_CONFIRMATION_REQUIRED", error.getBody().getProperties().get("code"));
            assertEquals(1L, error.getBody().getProperties().get("affectedRelationCount"));
            assertEquals(2L, error.getBody().getProperties().get("affectedEmployeeCount"));
            assertEquals(impact.toResponse().summary(), error.getBody().getDetail());
        }
        assertEquals(manager.getId(), subordinate.getManagerId());
        verify(users, never()).save(any());
    }

    @Test
    void confirmedImpactClearsOnlyManagerAndTimestampOnceForEachSubordinate() {
        subordinate.setManagerId(manager.getId());
        subordinate.setFullName("Nhân viên");
        LocalDateTime originalTime = LocalDateTime.of(2020, 1, 1, 0, 0);
        subordinate.setUpdatedAt(originalTime);
        UUID positionId = subordinate.getPositionId();
        User unaffected = employee(position(department, 1));
        unaffected.setManagerId(manager.getId());
        HierarchyImpact impact = new HierarchyImpact(Set.of(subordinate.getId()), Set.of(manager.getId(), subordinate.getId()));

        service.confirmOrClear(impact, true);

        assertNull(subordinate.getManagerId());
        assertTrue(subordinate.getUpdatedAt().isAfter(originalTime));
        assertEquals(positionId, subordinate.getPositionId());
        assertEquals("Nhân viên", subordinate.getFullName());
        assertEquals("FULL_TIME", subordinate.getEmploymentType());
        assertTrue(subordinate.getActive());
        assertFalse(subordinate.getLocked());
        assertEquals(manager.getId(), unaffected.getManagerId());
        verify(users, times(1)).save(subordinate);
        verify(users, times(1)).save(any());
    }

    @Test
    void impactDefensivelyCopiesIdsSoConfirmationCannotExpandAfterAnalysis() {
        Set<UUID> ids = new java.util.HashSet<>(Set.of(subordinate.getId()));
        Set<UUID> people = new java.util.HashSet<>(Set.of(manager.getId(), subordinate.getId()));
        HierarchyImpact impact = new HierarchyImpact(ids, people);
        ids.clear();
        people.clear();

        assertEquals(Set.of(subordinate.getId()), impact.invalidSubordinateIds());
        assertEquals(2L, impact.toResponse().affectedEmployeeCount());
        assertThrows(UnsupportedOperationException.class, () -> impact.invalidSubordinateIds().clear());
    }

    private void incidentQueries() {
        when(users.findAllByManagerIdAndDeletedAtIsNullOrderByFullNameAsc(any())).thenAnswer(call -> employees.values().stream()
                .filter(user -> call.getArgument(0).equals(user.getManagerId()) && user.getDeletedAt() == null).toList());
        when(users.findAllByPositionIdAndDeletedAtIsNull(any())).thenAnswer(call -> employees.values().stream()
                .filter(user -> call.getArgument(0).equals(user.getPositionId()) && user.getDeletedAt() == null).toList());
    }

    private Position position(UUID departmentId, int level) {
        Position position = Position.builder().id(UUID.randomUUID()).departmentId(departmentId)
                .hierarchyLevel(level).active(true).build();
        jobs.put(position.getId(), position);
        return position;
    }

    private User employee(Position position) {
        User employee = User.builder().id(UUID.randomUUID()).positionId(position.getId())
                .employmentType("FULL_TIME").active(true).locked(false).build();
        employees.put(employee.getId(), employee);
        return employee;
    }
}
