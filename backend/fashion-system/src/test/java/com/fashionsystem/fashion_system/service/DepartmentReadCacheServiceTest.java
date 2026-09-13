package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.DepartmentDto;
import com.fashionsystem.fashion_system.entity.Department;
import com.fashionsystem.fashion_system.mapper.DepartmentMapper;
import com.fashionsystem.fashion_system.repository.DepartmentRepository;
import com.fashionsystem.fashion_system.repository.UserDepartmentRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DepartmentReadCacheServiceTest.CacheTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DepartmentReadCacheServiceTest {
    private static final String DEPARTMENT_DETAIL_CACHE = "reference.department.detail";

    @Autowired DepartmentService departmentService;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired UserDepartmentRepository userDepartmentRepository;
    @Autowired CacheManager cacheManager;

    @BeforeEach
    void resetState() {
        reset(departmentRepository, userDepartmentRepository);
        cacheManager.getCache(DEPARTMENT_DETAIL_CACHE).clear();
    }

    @Test
    void departmentDetailReadsDatabaseOnlyOnce() {
        UUID id = UUID.randomUUID();
        Department department = Department.builder()
                .id(id).code("SALES").name("Kinh doanh").active(true).build();
        when(departmentRepository.findById(id)).thenReturn(Optional.of(department));

        DepartmentDto first = departmentService.getById(id);
        DepartmentDto second = departmentService.getById(id);

        assertThat(first.getCode()).isEqualTo("SALES");
        assertThat(second).isEqualTo(first);
        verify(departmentRepository, times(1)).findById(id);
    }

    @Test
    void departmentUpdateRefreshesCachedDetail() {
        UUID id = UUID.randomUUID();
        Department department = Department.builder()
                .id(id).code("SALES").name("Tên cũ").active(true).build();
        when(departmentRepository.findById(id)).thenReturn(Optional.of(department));
        when(departmentRepository.save(department)).thenReturn(department);

        assertThat(departmentService.getById(id).getName()).isEqualTo("Tên cũ");
        departmentService.update(id, DepartmentDto.builder()
                .code("IGNORED").name("Tên mới").active(true).build());

        assertThat(departmentService.getById(id).getName()).isEqualTo("Tên mới");
        verify(departmentRepository, times(2)).findById(id);
    }

    @Test
    void departmentSoftDeleteEvictsCachedDetail() {
        UUID id = UUID.randomUUID();
        Department department = Department.builder()
                .id(id).code("SALES").name("Kinh doanh").active(true).build();
        when(departmentRepository.findById(id)).thenReturn(Optional.of(department));
        when(departmentRepository.save(department)).thenReturn(department);
        when(userDepartmentRepository.existsActiveEmployeeByDepartmentId(id)).thenReturn(false);

        assertThat(departmentService.getById(id).getActive()).isTrue();
        departmentService.delete(id);

        assertThat(departmentService.getById(id).getActive()).isFalse();
        verify(departmentRepository, times(3)).findById(id);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class CacheTestConfiguration {
        @Bean CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(DEPARTMENT_DETAIL_CACHE);
        }
        @Bean DepartmentRepository departmentRepository() { return mock(DepartmentRepository.class); }
        @Bean UserDepartmentRepository userDepartmentRepository() { return mock(UserDepartmentRepository.class); }
        @Bean DepartmentMapper departmentMapper() { return new DepartmentMapper(); }
        @Bean DepartmentService departmentService(
                DepartmentRepository departmentRepository,
                DepartmentMapper departmentMapper,
                UserDepartmentRepository userDepartmentRepository) {
            return new DepartmentService(
                    departmentRepository, departmentMapper, userDepartmentRepository);
        }
    }
}
