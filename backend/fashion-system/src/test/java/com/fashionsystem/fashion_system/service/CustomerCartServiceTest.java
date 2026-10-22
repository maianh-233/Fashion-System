package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.storefront.CartDtos;
import com.fashionsystem.fashion_system.entity.CartItem;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.CartItemRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerCartServiceTest {
    final CartItemRepository repository = mock(CartItemRepository.class);
    final StorefrontCatalogService catalog = mock(StorefrontCatalogService.class);
    final CustomerCartService service = new CustomerCartService(repository, catalog);
    final UUID customer = UUID.randomUUID();
    final List<CartItem> rows = new ArrayList<>();
    final Map<UUID, CartDtos.VariantInfo> variants = new LinkedHashMap<>();

    final UUID shirt = UUID.randomUUID();
    final UUID soldOut = UUID.randomUUID();
    final UUID stopped = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        variants.put(shirt, info(shirt, "500000", "450000", 5, true));
        variants.put(soldOut, info(soldOut, "300000", null, 0, true));
        variants.put(stopped, info(stopped, "200000", null, 9, false));
        when(catalog.variants(anyCollection())).thenAnswer(call -> {
            Map<UUID, CartDtos.VariantInfo> result = new LinkedHashMap<>();
            ((Collection<?>) call.getArgument(0)).forEach(id -> {
                if (variants.containsKey(id)) result.put((UUID) id, variants.get(id));
            });
            return result;
        });
        when(repository.findAllByCustomerIdOrderByCreatedAtAsc(customer)).thenAnswer(call -> List.copyOf(rows));
        when(repository.findByCustomerIdAndProductVariantId(any(), any())).thenAnswer(call -> rows.stream()
                .filter(r -> r.getProductVariantId().equals(call.getArgument(1))).findFirst());
        when(repository.countByCustomerId(customer)).thenAnswer(call -> (long) rows.size());
        when(repository.save(any())).thenAnswer(call -> {
            CartItem item = call.getArgument(0);
            if (!rows.contains(item)) rows.add(item);
            return item;
        });
    }

    static CartDtos.VariantInfo info(UUID id, String price, String sale, int available, boolean sellable) {
        return new CartDtos.VariantInfo(id, UUID.randomUUID(), "slug", "Áo", "Brand", "SKU", "Đen", "M", null,
                new BigDecimal(price), sale == null ? null : new BigDecimal(sale), available, sellable);
    }

    @Test
    void addingTheSameVariantMergesQuantityUpToNinetyNine() {
        service.add(customer, new CartDtos.AddItem(shirt, 2));
        service.add(customer, new CartDtos.AddItem(shirt, 98));
        assertThat(rows).singleElement().satisfies(r -> assertThat(r.getQuantity()).isEqualTo(99));
    }

    @Test
    void stoppedProductsCannotBeAdded() {
        assertThatThrownBy(() -> service.add(customer, new CartDtos.AddItem(stopped, 1)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("ngừng bán");
        assertThat(rows).isEmpty();
    }

    @Test
    void cartIsPricedFromTheCatalogAndBlocksCheckoutForUnbuyableSelectedLines() {
        service.add(customer, new CartDtos.AddItem(shirt, 2));
        service.add(customer, new CartDtos.AddItem(soldOut, 1));

        CartDtos.Cart cart = service.view(customer);
        assertThat(cart.itemCount()).isEqualTo(3);
        assertThat(cart.items()).extracting(CartDtos.Line::problem).containsExactly(null, "Tạm hết hàng");
        assertThat(cart.items().getFirst().unitPrice()).isEqualByComparingTo("450000");
        // Chỉ dòng mua được mới vào tạm tính; còn dòng hết hàng được chọn thì chưa thanh toán được.
        assertThat(cart.selectedSubtotal()).isEqualByComparingTo("900000");
        assertThat(cart.canCheckout()).isFalse();

        service.update(customer, soldOut, new CartDtos.UpdateItem(null, false));
        assertThat(service.view(customer).canCheckout()).isTrue();
    }

    @Test
    void quantityAboveOnlineStockIsFlagged() {
        service.add(customer, new CartDtos.AddItem(shirt, 7));
        assertThat(service.view(customer).items().getFirst().problem()).isEqualTo("Chỉ còn 5 sản phẩm");
    }

    @Test
    void cartIsLimitedToFiftyDistinctLines() {
        for (int i = 0; i < CartDtos.MAX_LINES; i++) {
            UUID id = UUID.randomUUID();
            variants.put(id, info(id, "100000", null, 3, true));
            service.add(customer, new CartDtos.AddItem(id, 1));
        }
        assertThatThrownBy(() -> service.add(customer, new CartDtos.AddItem(shirt, 1)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("tối đa");
    }

    @Test
    void guestCartMergesAndSkipsStoppedProducts() {
        service.add(customer, new CartDtos.AddItem(shirt, 1));
        CartDtos.Cart merged = service.merge(customer, new CartDtos.GuestCart(List.of(
                new CartDtos.AddItem(shirt, 2), new CartDtos.AddItem(stopped, 1))));
        assertThat(merged.items()).singleElement().satisfies(line -> assertThat(line.quantity()).isEqualTo(3));
    }

    @Test
    void guestPreviewCombinesDuplicateLinesWithoutSaving() {
        CartDtos.Cart preview = service.preview(new CartDtos.GuestCart(List.of(
                new CartDtos.AddItem(shirt, 1), new CartDtos.AddItem(shirt, 2))));
        assertThat(preview.items()).singleElement().satisfies(line -> assertThat(line.quantity()).isEqualTo(3));
        assertThat(rows).isEmpty();
    }

    @Test
    void updatingAMissingLineIsNotFound() {
        when(repository.findByCustomerIdAndProductVariantId(customer, shirt)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(customer, shirt, new CartDtos.UpdateItem(2, null)))
                .isInstanceOf(BusinessException.class);
    }
}
