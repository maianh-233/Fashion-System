package com.fashion.inventoryservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fashion.inventoryservice.dto.common.GoodsReceiptItemDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptResponse {

    private UUID id;

    private String receiptCode;

    private UUID supplierId;

    private UUID storeId;

    private String status;

    private Integer totalQuantity;

    private BigDecimal totalAmount;

    private String note;

    private List<GoodsReceiptItemDto> items;

    private LocalDateTime receiptDate;
}