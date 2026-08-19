package com.fashionsystem.fashion_system.entity;

import java.util.UUID;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Class biểu diễn khóa chính kép của entity {@link ProductTagMapping}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProductTagMappingId implements Serializable {
    /** Phiên bản dùng để kiểm soát quá trình tuần tự hóa khóa chính. */
    private static final long serialVersionUID = 1L;

    /** Lưu thành phần product id của khóa chính. */
    private UUID productId;

    /** Lưu thành phần tag id của khóa chính. */
    private UUID tagId;

}
