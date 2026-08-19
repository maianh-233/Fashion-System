package com.fashionsystem.fashion_system.entity;

import java.util.UUID;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Class biểu diễn khóa chính kép của entity {@link PromotionCollection}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PromotionCollectionId implements Serializable {
    /** Phiên bản dùng để kiểm soát quá trình tuần tự hóa khóa chính. */
    private static final long serialVersionUID = 1L;

    /** Lưu thành phần promotion id của khóa chính. */
    private UUID promotionId;

    /** Lưu thành phần collection id của khóa chính. */
    private UUID collectionId;

}
