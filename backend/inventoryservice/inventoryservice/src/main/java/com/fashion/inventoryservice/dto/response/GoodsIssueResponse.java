package com.fashion.inventoryservice.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fashion.inventoryservice.dto.common.GoodsIssueItemDto;

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
public class GoodsIssueResponse {

    private UUID id;

    private String issueCode;

    private UUID storeId;

    private UUID orderId;

    private String issueType;

    private String status;

    private Integer totalQuantity;

    private String note;

    private List<GoodsIssueItemDto> items;

    private LocalDateTime issueDate;
}