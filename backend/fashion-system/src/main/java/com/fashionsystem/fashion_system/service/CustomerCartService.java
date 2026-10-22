package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.storefront.CartDtos;
import com.fashionsystem.fashion_system.entity.CartItem;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.CartItemRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giỏ hàng online của khách (bảng {@code cart_items}). Giỏ chỉ lưu biến thể + số lượng + đã chọn; giá, tồn kho và
 * trạng thái bán lấy từ catalog mỗi lần đọc nên luôn khớp với lúc thanh toán. Khách chưa đăng nhập giữ giỏ trên
 * trình duyệt; {@link #preview} dựng giỏ đó để hiển thị và {@link #merge} gộp vào giỏ tài khoản khi đăng nhập.
 */
@Service
@com.fashionsystem.fashion_system.audit.AuditInfrastructure(
        reason = "Shopping cart is customer working state; it never changes orders, stock or payments")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerCartService {
    private final CartItemRepository cartItems;
    private final StorefrontCatalogService catalog;

    public CartDtos.Cart view(UUID customerId) {
        return toCart(cartItems.findAllByCustomerIdOrderByCreatedAtAsc(customerId));
    }

    /** Giỏ trên trình duyệt (chưa đăng nhập): dựng như giỏ thật, không lưu. */
    public CartDtos.Cart preview(CartDtos.GuestCart guest) {
        Map<UUID, Integer> merged = new LinkedHashMap<>();
        guest.items().forEach(item -> merged.merge(item.variantId(), item.quantity(), (a, b) -> Math.min(CartDtos.MAX_QUANTITY, a + b)));
        LocalDateTime now = LocalDateTime.now();
        List<CartItem> lines = merged.entrySet().stream().map(e -> CartItem.builder().productVariantId(e.getKey())
                .quantity(e.getValue()).selected(true).createdAt(now).updatedAt(now).build()).toList();
        return toCart(lines);
    }

    /** Thêm vào giỏ; biến thể đã có thì cộng dồn (tối đa 99). Sản phẩm ngừng bán không thêm được. */
    @Transactional
    public CartDtos.Cart add(UUID customerId, CartDtos.AddItem request) {
        requireSellable(request.variantId());
        addOrMerge(customerId, request.variantId(), request.quantity(), LocalDateTime.now());
        return view(customerId);
    }

    @Transactional
    public CartDtos.Cart update(UUID customerId, UUID variantId, CartDtos.UpdateItem request) {
        CartItem item = cartItems.findByCustomerIdAndProductVariantId(customerId, variantId)
                .orElseThrow(() -> BusinessException.notFound("Sản phẩm không có trong giỏ hàng"));
        if (request.quantity() != null) item.setQuantity(request.quantity());
        if (request.selected() != null) item.setSelected(request.selected());
        item.setUpdatedAt(LocalDateTime.now());
        cartItems.save(item);
        return view(customerId);
    }

    @Transactional
    public CartDtos.Cart selectAll(UUID customerId, boolean selected) {
        LocalDateTime now = LocalDateTime.now();
        cartItems.findAllByCustomerIdOrderByCreatedAtAsc(customerId).forEach(item -> {
            item.setSelected(selected);
            item.setUpdatedAt(now);
            cartItems.save(item);
        });
        return view(customerId);
    }

    @Transactional
    public CartDtos.Cart remove(UUID customerId, UUID variantId) {
        cartItems.findByCustomerIdAndProductVariantId(customerId, variantId).ifPresent(cartItems::delete);
        return view(customerId);
    }

    /** Gộp giỏ trên trình duyệt vào giỏ tài khoản khi khách đăng nhập; dòng ngừng bán hoặc vượt giới hạn bị bỏ qua. */
    @Transactional
    public CartDtos.Cart merge(UUID customerId, CartDtos.GuestCart guest) {
        LocalDateTime now = LocalDateTime.now();
        Map<UUID, CartDtos.VariantInfo> info = catalog.variants(guest.items().stream().map(CartDtos.AddItem::variantId).toList());
        for (CartDtos.AddItem item : guest.items()) {
            CartDtos.VariantInfo variant = info.get(item.variantId());
            if (variant == null || !variant.sellable()) continue;
            try {
                addOrMerge(customerId, item.variantId(), item.quantity(), now);
            } catch (BusinessException full) {
                break;
            }
        }
        return view(customerId);
    }

    /** Sau khi đặt hàng: bỏ các biến thể đã đặt khỏi giỏ (trong cùng transaction đặt đơn). */
    @Transactional
    public void removeOrdered(UUID customerId, Collection<UUID> variantIds) {
        if (!variantIds.isEmpty()) cartItems.deleteVariants(customerId, variantIds);
    }

    private void addOrMerge(UUID customerId, UUID variantId, int quantity, LocalDateTime now) {
        CartItem item = cartItems.findByCustomerIdAndProductVariantId(customerId, variantId).orElse(null);
        if (item == null) {
            if (cartItems.countByCustomerId(customerId) >= CartDtos.MAX_LINES)
                throw BusinessException.badRequest("Giỏ hàng tối đa " + CartDtos.MAX_LINES + " sản phẩm khác nhau");
            item = CartItem.builder().customerId(customerId).productVariantId(variantId).quantity(0).selected(true)
                    .createdAt(now).build();
        }
        item.setQuantity(Math.min(CartDtos.MAX_QUANTITY, item.getQuantity() + quantity));
        item.setSelected(true);
        item.setUpdatedAt(now);
        cartItems.save(item);
    }

    private void requireSellable(UUID variantId) {
        CartDtos.VariantInfo variant = catalog.variants(List.of(variantId)).get(variantId);
        if (variant == null || !variant.sellable())
            throw BusinessException.badRequest("Sản phẩm không tồn tại hoặc đã ngừng bán");
    }

    private CartDtos.Cart toCart(List<CartItem> items) {
        Map<UUID, CartDtos.VariantInfo> info = catalog.variants(items.stream().map(CartItem::getProductVariantId).toList());
        List<CartDtos.Line> lines = new ArrayList<>();
        int itemCount = 0;
        int selectedCount = 0;
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean allSelectedBuyable = true;
        for (CartItem item : items) {
            CartDtos.VariantInfo v = info.get(item.getProductVariantId());
            if (v == null) continue;
            String problem = problem(v, item.getQuantity());
            BigDecimal lineTotal = v.unitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            boolean selected = Boolean.TRUE.equals(item.getSelected());
            lines.add(new CartDtos.Line(v.variantId(), v.productId(), v.slug(), v.productName(), v.brandName(), v.sku(),
                    v.color(), v.size(), v.imageUrl(), v.price(), v.salePrice(), v.unitPrice(), item.getQuantity(),
                    selected, v.available(), v.sellable(), lineTotal, problem));
            itemCount += item.getQuantity();
            if (selected) {
                selectedCount++;
                if (problem == null) subtotal = subtotal.add(lineTotal);
                else allSelectedBuyable = false;
            }
        }
        return new CartDtos.Cart(lines, itemCount, selectedCount, subtotal, selectedCount > 0 && allSelectedBuyable);
    }

    /** Lý do dòng không mua được với số lượng hiện tại; null nếu mua được. */
    static String problem(CartDtos.VariantInfo variant, int quantity) {
        if (!variant.sellable()) return "Sản phẩm đã ngừng bán";
        if (variant.available() <= 0) return "Tạm hết hàng";
        if (quantity > variant.available()) return "Chỉ còn " + variant.available() + " sản phẩm";
        return null;
    }
}
