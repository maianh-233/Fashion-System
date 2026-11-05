package com.fashionsystem.fashion_system.recruitment;

/** Trạng thái của hồ sơ trong một vòng tuyển dụng. */
public enum StageStatus {
    PENDING("Chưa bắt đầu"),
    IN_PROGRESS("Đang thực hiện"),
    AWAITING_EVALUATION("Chờ đánh giá"),
    PASSED("Đạt"),
    FAILED("Không đạt"),
    SKIPPED("Bỏ qua");

    private final String label;

    StageStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean decided() {
        return this == PASSED || this == FAILED || this == SKIPPED;
    }
}
