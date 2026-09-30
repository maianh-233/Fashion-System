package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.*;
import com.fashionsystem.fashion_system.repository.*;
import com.fashionsystem.fashion_system.mapper.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WarehouseStoreHttpTest {
    @Test void forgedStoreOnInventoryUrlReturns403FromServiceAuthority() throws Exception {
        UUID actor=UUID.randomUUID(), own=UUID.randomUUID(), other=UUID.randomUUID();
        var scope=mock(UserScopeService.class);
        when(scope.resolve(actor)).thenReturn(new UserScope(UserScope.Kind.STORE,own,"A","Store A"));
        var stores=mock(StoreRepository.class);
        var access=new StoreAccessService(scope,mock(AuthorizationService.class),stores);
        var balances=mock(InventoryBalanceRepository.class);
        var service=new InventoryService(balances,mock(InventoryTransactionRepository.class),stores,
            mock(ProductVariantRepository.class),mock(ProductRepository.class),new InventoryBalanceMapper(),new InventoryTransactionMapper(),access,scope);
        var mvc=MockMvcBuilders.standaloneSetup(new InventoryController(service)).build();
        var principal=new UsernamePasswordAuthenticationToken(new AuthenticatedUser(actor,"employee"),null,List.of());
        mvc.perform(get("/api/inventory/balances/"+other+"/"+UUID.randomUUID()).principal(principal)).andExpect(status().isForbidden());
        verifyNoInteractions(balances);
    }
}
