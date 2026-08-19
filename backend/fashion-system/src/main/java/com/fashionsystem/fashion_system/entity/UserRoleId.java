package com.fashionsystem.fashion_system.entity;

import java.util.UUID;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Class biểu diễn khóa chính kép của entity {@link UserRole}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserRoleId implements Serializable {
    /** Phiên bản dùng để kiểm soát quá trình tuần tự hóa khóa chính. */
    private static final long serialVersionUID = 1L;

    /** Lưu thành phần user id của khóa chính. */
    private UUID userId;

    /** Lưu thành phần role id của khóa chính. */
    private UUID roleId;

}
