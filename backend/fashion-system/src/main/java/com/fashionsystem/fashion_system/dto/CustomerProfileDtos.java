package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Hồ sơ của khách đang đăng nhập (trang "Hồ sơ của bạn"). */
public final class CustomerProfileDtos {
    private CustomerProfileDtos() {
    }

    /**
     * @param tierName    hạng thành viên hiện tại (null nếu chưa xếp hạng)
     * @param points      điểm tích lũy hiện có
     * @param hasPassword tài khoản đã có mật khẩu (tài khoản Google có thể chưa có)
     */
    public record Profile(UUID id, String customerCode, String username, String fullName, String email, String phone,
                          LocalDate dateOfBirth, String gender, String avatar, LocalDateTime createdAt,
                          String tierCode, String tierName, int points, boolean hasPassword) {
    }

    /**
     * Khách tự sửa hồ sơ. Đổi email cũng đổi email đăng nhập (dùng để nhận mã quên mật khẩu), nên cần
     * {@code currentPassword} khi tài khoản có mật khẩu.
     */
    public record UpdateProfile(
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(min = 2, max = 255) String fullName,
            @NotBlank(message = "Vui lòng nhập số điện thoại") @Size(max = 20) String phone,
            @Email(message = "Email không hợp lệ") @Size(max = 255) String email,
            @Past(message = "Ngày sinh phải ở trong quá khứ") LocalDate dateOfBirth,
            @Pattern(regexp = "MALE|FEMALE|OTHER") String gender,
            @Size(max = 72) String currentPassword) {
    }

    /** Đổi mật khẩu; tài khoản chưa có mật khẩu (đăng nhập Google) được đặt mới mà không cần mật khẩu cũ. */
    public record ChangePassword(
            @Size(max = 72) String currentPassword,
            @NotBlank(message = "Vui lòng nhập mật khẩu mới") @Size(min = 8, max = 72, message = "Mật khẩu từ 8 đến 72 ký tự")
            String newPassword) {
    }
}
