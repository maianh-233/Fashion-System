package com.fashionsystem.fashion_system.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fashionsystem.fashion_system.dto.SupplierDto;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.ProductAuthorizationService;
import com.fashionsystem.fashion_system.service.SupplierService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SupplierControllerTest {
    private SupplierService service;
    private ProductAuthorizationService authorization;
    private SupplierController controller;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(SupplierService.class);
        authorization = mock(ProductAuthorizationService.class);
        controller = new SupplierController(service, authorization);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void absentStatusDefaultsToActiveSupplierVisibility() throws Exception {
        when(authorization.visibleStatus(any(), eq(null))).thenReturn("ACTIVE");
        when(service.getList(eq(null), eq("ACTIVE"), any(Pageable.class))).thenReturn(Page.empty());

        controller.getList(authentication(), null, null, PageRequest.of(0, 20));

        verify(service).getList(eq(null), eq("ACTIVE"), any(Pageable.class));
    }

    @Test
    void rejectsInvalidSupplierPayloadBeforeCallingService() throws Exception {
        mvc.perform(post("/api/suppliers").principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Supplier","email":"not-an-email","phone":"0912345678"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAndRestoreDelegateToSoftDeleteLifecycle() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.restore(id)).thenReturn(SupplierDto.builder().id(id).status("ACTIVE").build());

        mvc.perform(delete("/api/suppliers/{id}", id).principal(authentication()))
                .andExpect(status().isNoContent());
        mvc.perform(patch("/api/suppliers/{id}/restore", id).principal(authentication()))
                .andExpect(status().isOk());

        verify(service).delete(id);
        verify(service).restore(id);
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(UUID.randomUUID(), "employee"), null, List.of());
    }
}
