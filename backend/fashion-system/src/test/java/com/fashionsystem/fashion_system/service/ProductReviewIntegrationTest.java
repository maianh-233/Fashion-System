package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.dto.StorageUploadResult;
import com.fashionsystem.fashion_system.dto.storefront.ReviewDtos;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

/** Đánh giá sản phẩm trên cơ sở dữ liệu thật; mọi dữ liệu do test tạo và rollback sau mỗi test. */
@SpringBootTest
@Transactional
class ProductReviewIntegrationTest {
    @Autowired ProductReviewService reviews;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean StorageService storage;

    final UUID store = UUID.randomUUID();
    final UUID product = UUID.randomUUID();
    final UUID variant = UUID.randomUUID();
    final UUID customer = UUID.randomUUID();
    final UUID otherCustomer = UUID.randomUUID();
    final UUID actor = UUID.randomUUID();
    UUID completedOrder;
    UUID pendingOrder;

    @BeforeEach
    void setUp() {
        String tag = product.toString().substring(0, 8);
        jdbc.update("insert into stores(id, code, name, active) values (?, ?, 'Zqrv Store', true)", store, "ZQRV-" + tag);
        jdbc.update("insert into products(id, code, name, slug, status, image_url) values (?, ?, 'Zqrv Áo len', ?, 'ACTIVE', '')",
                product, "ZQRV-" + tag, "zqrv-ao-len-" + tag);
        jdbc.update("insert into product_variants(id, product_id, sku, color, size, price, active) values (?, ?, ?, 'Be', 'L', 400000, true)",
                variant, product, "ZQRV-" + tag + "-L");
        jdbc.update("insert into users(id, username) values (?, ?)", actor, "zqrv-" + tag);
        customer(customer, "Nguyễn Minh Anh");
        customer(otherCustomer, "Trần Bình");
        completedOrder = order(customer, "COMPLETED");
        pendingOrder = order(customer, "CONFIRMED");
    }

    @Test
    void buyerReviewsACompletedOrderOnceAndItShowsUpPublicly() {
        assertThat(reviews.pending(customer)).extracting(ReviewDtos.Pending::orderId).containsExactly(completedOrder);

        ReviewDtos.MyReview created = asCustomer(() ->
                reviews.create(customer, completedOrder, product, 4, "  Len mềm, mặc ấm  ", List.of()));

        assertThat(created.rating()).isEqualTo(4);
        assertThat(created.content()).isEqualTo("Len mềm, mặc ấm");
        assertThat(created.variantLabel()).isEqualTo("Be / L");
        assertThat(reviews.pending(customer)).isEmpty();

        ReviewDtos.Summary summary = reviews.summary(product);
        assertThat(summary.count()).isEqualTo(1);
        assertThat(summary.average()).isEqualByComparingTo(new BigDecimal("4.0"));
        assertThat(summary.stars()).containsExactly(0L, 1L, 0L, 0L, 0L);

        var page = reviews.productReviews(product, null, false, Pageable.ofSize(5));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().reviewerName()).isEqualTo("N*** M*** Anh");
        assertThat(page.getContent().getFirst().verifiedPurchase()).isTrue();

        assertThatThrownBy(() -> asCustomer(() -> reviews.create(customer, completedOrder, product, 5, null, List.of())))
                .isInstanceOf(BusinessException.class).hasMessageContaining("đã đánh giá");
    }

    @Test
    void onlyTheBuyerOfACompletedOrderMayReview() {
        assertThatThrownBy(() -> asCustomer(() -> reviews.create(customer, pendingOrder, product, 5, null, List.of())))
                .isInstanceOf(BusinessException.class).hasMessageContaining("đã hoàn thành");
        assertThatThrownBy(() -> asCustomer(() -> reviews.create(otherCustomer, completedOrder, product, 5, null, List.of())))
                .isInstanceOf(BusinessException.class).hasMessageContaining("đã hoàn thành");
        assertThatThrownBy(() -> asCustomer(() -> reviews.create(customer, completedOrder, product, 6, null, List.of())))
                .isInstanceOf(BusinessException.class).hasMessageContaining("1 đến 5");
        assertThat(reviews.summary(product).count()).isZero();
    }

    @Test
    void staffHideWithReasonAndReply() {
        ReviewDtos.MyReview created = asCustomer(() ->
                reviews.create(customer, completedOrder, product, 1, "Giao sai màu", List.of()));

        assertThatThrownBy(() -> asCustomer(() ->
                reviews.setVisibility(created.id(), new ReviewDtos.VisibilityRequest(true, " "), actor)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("lý do");

        ReviewDtos.AdminReview replied = asCustomer(() ->
                reviews.reply(created.id(), new ReviewDtos.ReplyRequest("Shop xin lỗi, sẽ đổi hàng cho bạn"), actor));
        assertThat(replied.reply()).isEqualTo("Shop xin lỗi, sẽ đổi hàng cho bạn");
        assertThat(reviews.productReviews(product, 1, false, Pageable.ofSize(5)).getContent().getFirst().reply())
                .isEqualTo("Shop xin lỗi, sẽ đổi hàng cho bạn");

        ReviewDtos.AdminReview hidden = asCustomer(() ->
                reviews.setVisibility(created.id(), new ReviewDtos.VisibilityRequest(true, "Ngôn từ không phù hợp"), actor));
        assertThat(hidden.hidden()).isTrue();
        assertThat(hidden.hiddenReason()).isEqualTo("Ngôn từ không phù hợp");
        assertThat(reviews.summary(product).count()).isZero();
        assertThat(reviews.productReviews(product, null, false, Pageable.ofSize(5)).getTotalElements()).isZero();
        assertThat(reviews.mine(customer)).singleElement().extracting(ReviewDtos.MyReview::hidden).isEqualTo(true);
        assertThat(reviews.adminList("Zqrv", null, true, Pageable.ofSize(20)).getContent())
                .extracting(ReviewDtos.AdminReview::id).containsExactly(created.id());

        ReviewDtos.AdminReview shown = asCustomer(() ->
                reviews.setVisibility(created.id(), new ReviewDtos.VisibilityRequest(false, null), actor));
        assertThat(shown.hidden()).isFalse();
        assertThat(shown.hiddenReason()).isNull();
        assertThat(reviews.summary(product).count()).isEqualTo(1);
    }

    @Test
    void storesUpToThreePhotosInOrder() {
        when(storage.uploadImage(any(), startsWith("reviews/"))).thenAnswer(call -> {
            MultipartFile file = call.getArgument(0);
            return new StorageUploadResult("https://cdn.example/" + file.getOriginalFilename(), "pid-" + file.getOriginalFilename());
        });

        ReviewDtos.MyReview created = asCustomer(() -> reviews.create(customer, completedOrder, product, 5, "Đẹp",
                List.of(photo("a.jpg"), photo("b.jpg"))));

        assertThat(created.images()).containsExactly("https://cdn.example/a.jpg", "https://cdn.example/b.jpg");
        assertThat(reviews.summary(product).withImages()).isEqualTo(1);
        assertThat(reviews.productReviews(product, null, true, Pageable.ofSize(5)).getTotalElements()).isEqualTo(1);
    }

    @Test
    void rejectsMoreThanThreePhotosBeforeUploading() {
        assertThatThrownBy(() -> asCustomer(() -> reviews.create(customer, completedOrder, product, 5, null,
                List.of(photo("1.jpg"), photo("2.jpg"), photo("3.jpg"), photo("4.jpg")))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Tối đa 3 ảnh");
        verifyNoInteractions(storage);
    }

    private static MultipartFile photo(String name) {
        return new MockMultipartFile("images", name, "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1});
    }

    @Test
    void masksReviewerNames() {
        assertThat(ProductReviewService.maskName("Nguyễn Minh Anh")).isEqualTo("N*** M*** Anh");
        assertThat(ProductReviewService.maskName("Lan")).isEqualTo("L***");
        assertThat(ProductReviewService.maskName(null)).isEqualTo("Khách hàng");
    }

    private <T> T asCustomer(Supplier<T> action) {
        try (var ignored = BusinessAuditContextFactory.systemJob("TEST_REVIEW")) {
            return action.get();
        }
    }

    private void customer(UUID id, String name) {
        jdbc.update("insert into customers(id, customer_code, membership_status, source, full_name) values (?, ?, 'MEMBER', 'WEBSITE', ?)",
                id, "ZQRV-" + id.toString().substring(0, 8), name);
    }

    private UUID order(UUID customerId, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into orders(id, order_code, order_type, status, active, store_id, customer_id, subtotal, discount_total, "
                        + "shipping_fee, total_amount, payment_status, created_at) values (?, ?, 'ONLINE', ?, true, ?, ?, 400000, 0, 0, "
                        + "400000, 'PAID', now() at time zone 'UTC')",
                id, "ZQRV-" + id.toString().substring(0, 12), status, store, customerId);
        jdbc.update("insert into order_items(order_id, product_id, product_variant_id, product_name, color, size, price, quantity, total) "
                        + "values (?, ?, ?, 'Zqrv Áo len', 'Be', 'L', 400000, 1, 400000)", id, product, variant);
        return id;
    }
}
