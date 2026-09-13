package com.fashionsystem.fashion_system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fashionsystem.fashion_system.entity.Department;
import com.fashionsystem.fashion_system.entity.Position;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.mapper.PositionMapper;
import com.fashionsystem.fashion_system.repository.DepartmentRepository;
import com.fashionsystem.fashion_system.repository.PositionRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import com.fashionsystem.fashion_system.service.OrganizationHierarchyService;
import com.fashionsystem.fashion_system.service.PositionService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitConfig(PositionControllerSecurityTest.TestConfiguration.class)
@WebAppConfiguration
class PositionControllerSecurityTest {
    @Autowired WebApplicationContext context;
    @Autowired PositionRepository positions;
    @Autowired DepartmentRepository departments;
    @Autowired UserRepository users;
    private MockMvc mvc;
    private UUID departmentId;
    private Position position;
    private User manager;
    private User subordinate;

    @BeforeEach
    void setup() {
        reset(positions, departments, users);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        departmentId = UUID.randomUUID();
        position = Position.builder().id(UUID.randomUUID()).departmentId(departmentId).code("LEAD")
                .name("Lead").active(true).hierarchyLevel(3).minSalary(10L).maxSalary(20L).build();
        Position staff = Position.builder().id(UUID.randomUUID()).departmentId(departmentId).hierarchyLevel(2).build();
        manager = User.builder().id(UUID.randomUUID()).positionId(position.getId()).employmentType("FULL_TIME").build();
        subordinate = User.builder().id(UUID.randomUUID()).positionId(staff.getId()).managerId(manager.getId()).build();
        when(positions.findById(position.getId())).thenReturn(Optional.of(position));
        when(positions.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(departments.findById(departmentId)).thenReturn(Optional.of(Department.builder().id(departmentId).active(true).build()));
        when(users.findAllByPositionIdAndDeletedAtIsNull(position.getId())).thenReturn(List.of(manager));
        when(users.findAllByManagerIdAndDeletedAtIsNullOrderByFullNameAsc(manager.getId())).thenReturn(List.of(subordinate));
        when(users.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(users.findById(subordinate.getId())).thenReturn(Optional.of(subordinate));
    }

    @Test
    void previewWithoutPositionUpdateIsForbidden() throws Exception {
        mvc.perform(get(previewUrl()).param("departmentId", departmentId.toString()).param("hierarchyLevel", "2")
                .with(user("viewer").authorities(new SimpleGrantedAuthority("POSITION_VIEW"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(positions, departments, users);
    }

    @Test
    void authorizedPreviewReturnsImpactWithoutChangingRelations() throws Exception {
        mvc.perform(get(previewUrl()).param("departmentId", departmentId.toString()).param("hierarchyLevel", "2")
                .with(user("editor").authorities(new SimpleGrantedAuthority("POSITION_UPDATE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedRelationCount").value(1))
                .andExpect(jsonPath("$.affectedEmployeeCount").value(2));
        assertEquals(manager.getId(), subordinate.getManagerId());
        assertEquals(3, position.getHierarchyLevel());
        verify(users, never()).save(any());
        verify(positions, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "not-a-number", "2147483648"})
    void previewRejectsInvalidNumericLevel(String level) throws Exception {
        mvc.perform(get(previewUrl()).param("departmentId", departmentId.toString()).param("hierarchyLevel", level)
                .with(user("editor").authorities(new SimpleGrantedAuthority("POSITION_UPDATE"))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(positions, departments, users);
    }

    @Test
    void unconfirmedUpdateSerializesStructured409OverHttp() throws Exception {
        mvc.perform(put("/api/positions/{id}", position.getId())
                .with(user("editor").authorities(new SimpleGrantedAuthority("POSITION_UPDATE")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"departmentId":"%s","name":"Lead","active":true,
                         "hierarchyLevel":2,"minSalary":10,"maxSalary":20}
                        """.formatted(departmentId)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("HIERARCHY_CONFIRMATION_REQUIRED"))
                .andExpect(jsonPath("$.affectedRelationCount").value(1))
                .andExpect(jsonPath("$.affectedEmployeeCount").value(2));
        assertEquals(3, position.getHierarchyLevel());
        assertEquals(manager.getId(), subordinate.getManagerId());
        verify(users, never()).save(any());
        verify(positions, never()).save(any());
    }

    private String previewUrl() { return "/api/positions/" + position.getId() + "/hierarchy-impact"; }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    @Import({PositionController.class, PositionService.class, PositionMapper.class, OrganizationHierarchyService.class})
    static class TestConfiguration {
        @Bean PositionRepository positions() { return mock(PositionRepository.class); }
        @Bean DepartmentRepository departments() { return mock(DepartmentRepository.class); }
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).build();
        }
    }
}
