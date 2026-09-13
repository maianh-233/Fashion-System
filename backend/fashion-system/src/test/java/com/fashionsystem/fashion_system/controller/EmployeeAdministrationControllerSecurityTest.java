package com.fashionsystem.fashion_system.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.EmployeeAdministrationService;
import com.fashionsystem.fashion_system.dto.employee.SubordinateAssignmentRequest;
import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.repository.*;
import com.fashionsystem.fashion_system.service.*;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.StoreMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Xác minh controller chỉ truyền principal; service tự resolve data scope từ permission. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EmployeeAdministrationControllerSecurityTest.TestConfiguration.class)
class EmployeeAdministrationControllerSecurityTest {
    @Autowired EmployeeAdministrationController controller;
    @Autowired EmployeeAdministrationService service;
    private UUID actorId;

    @BeforeEach void reset() { clearInvocations(service); actorId = UUID.randomUUID(); }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void missingUserViewIsForbidden() {
        authenticate("ROLE_MANAGER");
        assertThrows(AccessDeniedException.class, () -> list());
        verifyNoInteractions(service);
    }

    @Test
    void roleNameDoesNotChangeEmployeeServiceSignature() {
        authenticate("ROLE_MANAGER", "USER_VIEW");
        assertDoesNotThrow(this::list);
        verify(service).getList(eq(actorId), isNull(), isNull(), isNull(), isNull(), any());
        clearInvocations(service);
        authenticate("ROLE_ADMIN", "USER_VIEW");
        assertDoesNotThrow(this::list);
        verify(service).getList(eq(actorId), isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    void scopeEndpointUsesAuthenticatedActorId() {
        authenticate("ROLE_MANAGER", "USER_VIEW");
        assertDoesNotThrow(() -> controller.getScope(SecurityContextHolder.getContext().getAuthentication()));
        verify(service).getScope(actorId);
    }

    @Test
    void managerCanViewSubordinatesWithinStoreScope() {
        UUID managerId = UUID.randomUUID();
        authenticate("ROLE_MANAGER", "USER_VIEW");
        assertDoesNotThrow(() -> controller.getSubordinates(
                SecurityContextHolder.getContext().getAuthentication(), managerId));
        verify(service).getSubordinates(actorId, managerId);
    }

    @Test
    void assigningSubordinateRequiresUserUpdate() {
        UUID managerId = UUID.randomUUID();
        authenticate("ROLE_MANAGER", "USER_VIEW");
        assertThrows(AccessDeniedException.class, () -> controller.assignSubordinate(
                SecurityContextHolder.getContext().getAuthentication(), managerId,
                new SubordinateAssignmentRequest("staff@example.com")));
        verifyNoInteractions(service);
    }

    private void list() { controller.getList(SecurityContextHolder.getContext().getAuthentication(),
            null, null, null, null, PageRequest.of(0, 10)); }
    private void authenticate(String... authorities) {
        var granted = java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
        var principal = new AuthenticatedUser(actorId, "employee");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, granted));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfiguration {
        @Bean EmployeeAdministrationService employeeAdministrationService() { return mock(EmployeeAdministrationService.class); }
        @Bean EmployeeAdministrationController employeeAdministrationController(EmployeeAdministrationService service) {
            return new EmployeeAdministrationController(service);
        }
    }
}

@SpringJUnitConfig(EmployeeHierarchyHttpTest.HttpConfiguration.class)
@WebAppConfiguration
class EmployeeHierarchyHttpTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PositionRepository positions;
    @Autowired DepartmentRepository departments;
    @Autowired RoleRepository roles;
    @Autowired EmployeeDataScopeService scopes;
    private MockMvc mvc;
    private UUID actorId;
    private UUID departmentId;
    private User employee;
    private User child;
    private Position current;
    private Position proposed;

    @BeforeEach
    void setup() {
        org.mockito.Mockito.reset(users, positions, departments, roles, scopes);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        actorId = UUID.randomUUID();
        departmentId = UUID.randomUUID();
        current = Position.builder().id(UUID.randomUUID()).departmentId(departmentId).hierarchyLevel(3)
                .name("Manager").active(true).build();
        proposed = Position.builder().id(UUID.randomUUID()).departmentId(departmentId).hierarchyLevel(2)
                .name("Staff").active(true).build();
        employee = User.builder().id(UUID.randomUUID()).username("employee").employeeCode("NV01")
                .fullName("Employee").email("employee@example.com").phone("0912345678")
                .positionId(current.getId()).employmentType("FULL_TIME").active(true).locked(false).build();
        child = User.builder().id(UUID.randomUUID()).managerId(employee.getId()).positionId(proposed.getId())
                .fullName("Child").active(true).locked(false).build();
        when(users.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(users.findById(child.getId())).thenReturn(Optional.of(child));
        when(users.findAllByManagerIdAndDeletedAtIsNullOrderByFullNameAsc(employee.getId())).thenReturn(List.of(child));
        when(positions.findById(current.getId())).thenReturn(Optional.of(current));
        when(positions.findById(proposed.getId())).thenReturn(Optional.of(proposed));
        when(departments.findById(departmentId)).thenReturn(Optional.of(Department.builder().id(departmentId).active(true).build()));
        when(scopes.resolve(actorId, "USER_UPDATE")).thenReturn(EmployeeDataScope.all("USER_UPDATE"));
        when(roles.findAllByCodeIn(Set.of("STAFF"))).thenReturn(List.of(Role.builder().id(UUID.randomUUID()).code("STAFF").build()));
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void bothHierarchyEndpointsRequireUserUpdate() throws Exception {
        mvc.perform(get(url("hierarchy-impact")).with(asActor("USER_VIEW"))
                        .param("departmentId", departmentId.toString()).param("positionId", proposed.getId().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(get(url("eligible-subordinates")).with(asActor("USER_VIEW")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(users, positions, departments, scopes);
    }

    @Test
    void hierarchyPreviewReturnsCountsWithoutMutatingTheEmployee() throws Exception {
        mvc.perform(get(url("hierarchy-impact")).with(asActor("USER_UPDATE"))
                        .param("departmentId", departmentId.toString()).param("positionId", proposed.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedRelationCount").value(1))
                .andExpect(jsonPath("$.affectedEmployeeCount").value(2));
        assertEquals(current.getId(), employee.getPositionId());
        assertEquals(employee.getId(), child.getManagerId());
        verify(users, never()).save(any());
        verify(scopes).requireTarget(any(), eq(employee.getId()));
    }

    @Test
    void missingOrMalformedPreviewOrganizationReturns400() throws Exception {
        mvc.perform(get(url("hierarchy-impact")).with(asActor("USER_UPDATE"))
                        .param("departmentId", departmentId.toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get(url("hierarchy-impact")).with(asActor("USER_UPDATE"))
                        .param("departmentId", "invalid").param("positionId", proposed.getId().toString()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(users, positions, scopes);
    }

    @Test
    void hierarchyEndpointsEnforceTargetStoreScope() throws Exception {
        doThrow(BusinessException.forbidden("cross store")).when(scopes).requireTarget(any(), eq(employee.getId()));
        mvc.perform(get(url("hierarchy-impact")).with(asActor("USER_UPDATE"))
                        .param("departmentId", departmentId.toString()).param("positionId", proposed.getId().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(get(url("eligible-subordinates")).with(asActor("USER_UPDATE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(positions, departments);
        verify(users, never()).findEligibleSubordinates(any(), any(), any(), any());
    }

    @Test
    void candidateEndpointUsesServerPositionAndActorStoreDespiteClientFilters() throws Exception {
        UUID storeId = UUID.randomUUID();
        var scope = EmployeeDataScope.store(Store.builder().id(storeId).code("A").name("A").active(true).build(), "USER_UPDATE");
        when(scopes.resolve(actorId, "USER_UPDATE")).thenReturn(scope);
        child.setManagerId(null);
        when(users.findEligibleSubordinates(employee.getId(), departmentId, 3, storeId)).thenReturn(List.of(child));
        mvc.perform(get(url("eligible-subordinates")).with(asActor("USER_UPDATE"))
                        .param("departmentId", UUID.randomUUID().toString()).param("managerLevel", "99999")
                        .param("storeId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(child.getId().toString()))
                .andExpect(jsonPath("$[0].positionName").value("Staff"))
                .andExpect(jsonPath("$[0].hierarchyLevel").value(2));
        verify(users).findEligibleSubordinates(employee.getId(), departmentId, 3, storeId);
        verify(users, never()).save(any());
    }

    @Test
    void unconfirmedEmployeeUpdateReturnsStructured409JsonAndKeepsRelations() throws Exception {
        mvc.perform(put("/api/employees/{id}", employee.getId()).with(asActor("USER_UPDATE"))
                        .contentType(MediaType.APPLICATION_JSON).content(updateJson(false)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("HIERARCHY_CONFIRMATION_REQUIRED"))
                .andExpect(jsonPath("$.affectedRelationCount").value(1))
                .andExpect(jsonPath("$.affectedEmployeeCount").value(2));
        assertEquals(current.getId(), employee.getPositionId());
        assertEquals(employee.getId(), child.getManagerId());
        verify(users, never()).save(any());
    }

    @Test
    void confirmedEmployeeUpdateClearsInvalidRelationOverHttp() throws Exception {
        mvc.perform(put("/api/employees/{id}", employee.getId()).with(asActor("USER_UPDATE"))
                        .contentType(MediaType.APPLICATION_JSON).content(updateJson(true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionId").value(proposed.getId().toString()));
        org.junit.jupiter.api.Assertions.assertNull(child.getManagerId());
        assertEquals(proposed.getId(), employee.getPositionId());
    }

    private String updateJson(boolean confirmed) {
        return """
                {"username":"employee","fullName":"Employee","email":"employee@example.com","phone":"0912345678",
                 "employmentType":"FULL_TIME","departmentId":"%s","positionId":"%s","active":true,"locked":false,
                 "roleCodes":["STAFF"]%s}
                """.formatted(departmentId, proposed.getId(), confirmed ? ",\"resetInvalidRelations\":true" : "");
    }
    private String url(String suffix) { return "/api/employees/" + employee.getId() + "/" + suffix; }
    private org.springframework.test.web.servlet.request.RequestPostProcessor asActor(String authority) {
        return authentication(new UsernamePasswordAuthenticationToken(new AuthenticatedUser(actorId, "actor"),
                null, List.of(new SimpleGrantedAuthority(authority))));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc @EnableWebSecurity @EnableMethodSecurity
    @Import({EmployeeAdministrationController.class, EmployeeAdministrationService.class, OrganizationHierarchyService.class})
    static class HttpConfiguration {
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean PositionRepository positions() { return mock(PositionRepository.class); }
        @Bean DepartmentRepository departments() { return mock(DepartmentRepository.class); }
        @Bean RoleRepository roles() { return mock(RoleRepository.class); }
        @Bean UserRoleRepository userRoles() { return mock(UserRoleRepository.class); }
        @Bean UserDepartmentRepository userDepartments() { return mock(UserDepartmentRepository.class); }
        @Bean StoreRepository stores() { return mock(StoreRepository.class); }
        @Bean StoreStaffRepository staffs() { return mock(StoreStaffRepository.class); }
        @Bean PasswordEncoder passwords() { return mock(PasswordEncoder.class); }
        @Bean StoreMapper storeMapper() { return mock(StoreMapper.class); }
        @Bean AuthAuditService authAudit() { return mock(AuthAuditService.class); }
        @Bean AuditLogService audit() { return mock(AuditLogService.class); }
        @Bean EmployeeDataScopeService scope() { return mock(EmployeeDataScopeService.class); }
        @Bean AuthorizationService authorization() { return mock(AuthorizationService.class); }
        @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).build();
        }
    }
}
