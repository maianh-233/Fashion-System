package com.fashionsystem.fashion_system.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

class AuditMutationCoverageTest {
    @Test
    void businessBulkRepositoriesUseEntityCallbacksExceptRecordedInventoryInitialization() {
        for (Class<?> repository : new Class<?>[]{
                com.fashionsystem.fashion_system.repository.ProductRepository.class,
                com.fashionsystem.fashion_system.repository.ProductVariantRepository.class,
                com.fashionsystem.fashion_system.repository.ProductImageRepository.class,
                com.fashionsystem.fashion_system.repository.RolePermissionRepository.class,
                com.fashionsystem.fashion_system.repository.UserRoleRepository.class,
                com.fashionsystem.fashion_system.repository.UserPermissionRepository.class,
                com.fashionsystem.fashion_system.repository.CustomerAddressRepository.class,
                com.fashionsystem.fashion_system.repository.CustomerTierAssignmentRepository.class}) {
            assertThat(repository.getDeclaredMethods()).as(repository.getSimpleName())
                    .noneMatch(method -> method.isAnnotationPresent(org.springframework.data.jpa.repository.Modifying.class));
        }
    }

    @Test
    void everyPublicWriteTransactionHasAnAuditClassification() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Service.class));
        var uncovered = new ArrayList<String>();
        for (var bean : scanner.findCandidateComponents("com.fashionsystem.fashion_system.service")) {
            Class<?> type = Class.forName(bean.getBeanClassName());
            for (var method : type.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())) continue;
                Transactional tx = method.getAnnotation(Transactional.class);
                if (tx == null) tx = type.getAnnotation(Transactional.class);
                if (tx == null || tx.readOnly()) continue;
                boolean classified = java.util.stream.Stream.concat(
                        java.util.Arrays.stream(type.getAnnotations()), java.util.Arrays.stream(method.getAnnotations()))
                        .anyMatch(a -> a.annotationType().getSimpleName().equals("BusinessAudit")
                                || a.annotationType().getSimpleName().equals("AuditInfrastructure"));
                if (!classified) uncovered.add(type.getSimpleName() + "." + method.getName());
            }
        }
        assertThat(uncovered).as("Uncovered mutation boundaries").isEmpty();
    }
}
