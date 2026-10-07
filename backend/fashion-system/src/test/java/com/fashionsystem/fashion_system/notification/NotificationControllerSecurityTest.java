package com.fashionsystem.fashion_system.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;

class NotificationControllerSecurityTest {

    @Test
    void scopesHistoryToAuthenticatedEmployeeOrCustomerNamespace() {
        NotificationQueryService service = mock(NotificationQueryService.class);
        NotificationController controller = new NotificationController(service);
        UUID userId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(service.history(RealtimeIdentity.user(userId), 0, 20)).thenReturn(Page.empty());
        when(service.history(RealtimeIdentity.customer(customerId), 0, 20)).thenReturn(Page.empty());

        controller.history(new AuthenticatedUser(userId, "employee"), 0, 20);
        controller.history(new AuthenticatedCustomer(customerId, "customer"), 0, 20);

        verify(service).history(RealtimeIdentity.user(userId), 0, 20);
        verify(service).history(RealtimeIdentity.customer(customerId), 0, 20);
    }

    @Test
    void endpointExposesNoArbitraryRecipientParameter() {
        Method history = java.util.Arrays.stream(NotificationController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("history"))
                .findFirst()
                .orElseThrow();

        assertThat(history.getParameters())
                .filteredOn(parameter -> parameter.isAnnotationPresent(RequestParam.class))
                .allSatisfy(parameter -> assertThat(parameter.getName()).isIn("page", "size"));
    }
}
