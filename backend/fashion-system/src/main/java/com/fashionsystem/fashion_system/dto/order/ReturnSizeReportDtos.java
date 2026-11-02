package com.fashionsystem.fashion_system.dto.order;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Báo cáo tỷ lệ đổi / trả theo sản phẩm và size để chỉnh bảng size. Đơn bán là đơn cửa hàng đã hoàn thành, tạo trong
 * kỳ; hàng đổi / trả là các món của chính những đơn đó nằm trong yêu cầu đã duyệt hoặc đã hoàn tất (cả trả lẫn đổi).
 * Tỷ lệ tính theo số món, đơn vị phần trăm.
 */
public final class ReturnSizeReportDtos {
    private ReturnSizeReportDtos() {
    }

    /** Khách đổi sang size lớn hơn nhiều hơn (form nhỏ) hoặc nhỏ hơn nhiều hơn (form rộng). */
    public enum FitHint { RUNS_SMALL, RUNS_LARGE }

    /**
     * @param flaggedProducts số sản phẩm nên xem lại bảng size
     */
    public record Report(LocalDate from, LocalDate to, Summary summary, List<ReasonCount> reasons,
                         List<ProductRow> products, int flaggedProducts) {
    }

    public record Summary(long soldQuantity, long returnedQuantity, long wrongSizeQuantity, double returnRate,
                          double wrongSizeRate) {
    }

    public record ReasonCount(String code, String label, long quantity) {
    }

    /**
     * @param flagged  tỷ lệ "không vừa size" đạt ngưỡng cần xem lại bảng size
     * @param fitHint  xu hướng đổi size của cả sản phẩm (null khi chưa rõ)
     */
    public record ProductRow(UUID productId, String productName, UUID categoryId, String categoryName,
                             long soldQuantity, long returnedQuantity, long wrongSizeQuantity, double returnRate,
                             double wrongSizeRate, boolean flagged, FitHint fitHint, List<SizeRow> sizes) {
    }

    /**
     * @param exchangedUp   số món "không vừa size" khách đổi sang size lớn hơn
     * @param exchangedDown số món "không vừa size" khách đổi sang size nhỏ hơn
     * @param exchanges     size khách đổi sang (mọi lý do), nhiều nhất trước
     */
    public record SizeRow(String size, long soldQuantity, long returnedQuantity, long wrongSizeQuantity,
                          double returnRate, double wrongSizeRate, long exchangedUp, long exchangedDown,
                          FitHint fitHint, List<ExchangeFlow> exchanges) {
    }

    public record ExchangeFlow(String toSize, long quantity) {
    }
}
