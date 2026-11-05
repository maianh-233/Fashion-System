package com.fashionsystem.fashion_system.recruitment;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Trạng thái tổng thể của một hồ sơ ứng tuyển (khác trạng thái từng vòng {@link StageStatus}). Chuyển trạng thái thủ
 * công chỉ theo bảng {@link #allowedNext()}; các thao tác nghiệp vụ (đánh giá, phỏng vấn, offer) tự đẩy trạng thái.
 */
public enum ApplicationStatus {
    NEW("Mới tiếp nhận"),
    SCREENING_PENDING("Chờ sàng lọc"),
    PROCESSING("Đang xử lý"),
    QUALIFIED("Đạt yêu cầu"),
    REJECTED("Không đạt"),
    INTERVIEW_PENDING("Chờ phỏng vấn"),
    INTERVIEWING("Đang phỏng vấn"),
    EVALUATION_PENDING("Chờ đánh giá"),
    ROUND_PASSED("Đạt vòng tuyển dụng"),
    ROUND_FAILED("Không đạt vòng tuyển dụng"),
    OFFER_PENDING("Chờ Offer"),
    OFFER_SENT("Đã gửi Offer"),
    OFFER_ACCEPTED("Đã chấp nhận Offer"),
    OFFER_DECLINED("Từ chối Offer"),
    HIRED("Đã tuyển dụng"),
    WITHDRAWN("Ứng viên rút hồ sơ");

    private final String label;

    ApplicationStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Không còn xử lý tiếp. REJECTED / OFFER_DECLINED vẫn có thể mở lại có lý do. */
    public boolean closed() {
        return this == REJECTED || this == HIRED || this == WITHDRAWN || this == OFFER_DECLINED;
    }

    /** Đã qua toàn bộ quy trình (dùng cho thống kê "đạt"). */
    public boolean passedProcess() {
        return PASSED.contains(this);
    }

    /** Trạng thái do thao tác offer / tuyển dụng quyết định, không được đặt tay. */
    public boolean systemOnly() {
        return this == OFFER_SENT || this == OFFER_ACCEPTED || this == OFFER_DECLINED || this == HIRED;
    }

    public Set<ApplicationStatus> allowedNext() {
        return TRANSITIONS.getOrDefault(this, Set.of());
    }

    public boolean canMoveTo(ApplicationStatus target) {
        return allowedNext().contains(target);
    }

    private static final Set<ApplicationStatus> PASSED =
            EnumSet.of(QUALIFIED, OFFER_PENDING, OFFER_SENT, OFFER_ACCEPTED, OFFER_DECLINED, HIRED);

    /** Ai cũng có thể rút hồ sơ / bị loại khi chưa kết thúc; các bước còn lại đi theo quy trình. */
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> TRANSITIONS = Map.ofEntries(
            Map.entry(NEW, EnumSet.of(SCREENING_PENDING, PROCESSING, QUALIFIED, REJECTED, WITHDRAWN)),
            Map.entry(SCREENING_PENDING, EnumSet.of(PROCESSING, QUALIFIED, REJECTED, WITHDRAWN)),
            Map.entry(PROCESSING, EnumSet.of(QUALIFIED, REJECTED, INTERVIEW_PENDING, EVALUATION_PENDING,
                    ROUND_PASSED, ROUND_FAILED, OFFER_PENDING, WITHDRAWN)),
            Map.entry(QUALIFIED, EnumSet.of(PROCESSING, INTERVIEW_PENDING, OFFER_PENDING, REJECTED, WITHDRAWN)),
            Map.entry(INTERVIEW_PENDING, EnumSet.of(INTERVIEWING, EVALUATION_PENDING, REJECTED, WITHDRAWN)),
            Map.entry(INTERVIEWING, EnumSet.of(EVALUATION_PENDING, INTERVIEW_PENDING, REJECTED, WITHDRAWN)),
            Map.entry(EVALUATION_PENDING, EnumSet.of(ROUND_PASSED, ROUND_FAILED, INTERVIEW_PENDING, REJECTED, WITHDRAWN)),
            Map.entry(ROUND_PASSED, EnumSet.of(PROCESSING, INTERVIEW_PENDING, OFFER_PENDING, QUALIFIED, REJECTED, WITHDRAWN)),
            Map.entry(ROUND_FAILED, EnumSet.of(REJECTED, PROCESSING, WITHDRAWN)),
            Map.entry(OFFER_PENDING, EnumSet.of(REJECTED, WITHDRAWN)),
            Map.entry(OFFER_SENT, EnumSet.of(WITHDRAWN)),
            Map.entry(OFFER_ACCEPTED, EnumSet.of(WITHDRAWN)),
            Map.entry(OFFER_DECLINED, EnumSet.of(OFFER_PENDING, REJECTED)),
            Map.entry(REJECTED, EnumSet.of(PROCESSING)));
}
