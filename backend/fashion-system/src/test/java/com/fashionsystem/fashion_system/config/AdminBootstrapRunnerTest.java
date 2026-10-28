package com.fashionsystem.fashion_system.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.entity.Role;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.entity.UserRole;
import com.fashionsystem.fashion_system.repository.RoleRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import com.fashionsystem.fashion_system.repository.UserRoleRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserRoleRepository userRoleRepository;
    @Mock PasswordEncoder passwordEncoder;

    private final Role superAdmin = Role.builder().id(UUID.randomUUID()).code("SUPER_ADMIN").build();

    @Test
    void doesNothingWhenCredentialsAreNotConfigured() {
        runner("", "").run(null);

        verifyNoInteractions(roleRepository, userRepository, userRoleRepository);
    }

    @Test
    void createsSuperAdminOnEmptyDatabase() {
        UUID userId = UUID.randomUUID();
        when(roleRepository.findByCode("SUPER_ADMIN")).thenReturn(Optional.of(superAdmin));
        when(userRoleRepository.existsByRoleId(superAdmin.getId())).thenReturn(false);
        when(userRepository.findByUsername("root")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });

        runner(" Root ", "secret").run(null);

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(user.capture());
        assertThat(user.getValue().getUsername()).isEqualTo("root");
        assertThat(user.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(user.getValue().getActive()).isTrue();
        ArgumentCaptor<UserRole> link = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(link.capture());
        assertThat(link.getValue().getUserId()).isEqualTo(userId);
        assertThat(link.getValue().getRoleId()).isEqualTo(superAdmin.getId());
    }

    @Test
    void skipsWhenASuperAdminAlreadyExists() {
        when(roleRepository.findByCode("SUPER_ADMIN")).thenReturn(Optional.of(superAdmin));
        when(userRoleRepository.existsByRoleId(superAdmin.getId())).thenReturn(true);

        runner("root", "secret").run(null);

        verify(userRepository, never()).save(any());
    }

    private AdminBootstrapRunner runner(String username, String password) {
        return new AdminBootstrapRunner(
                userRepository, roleRepository, userRoleRepository, passwordEncoder, username, password);
    }
}
