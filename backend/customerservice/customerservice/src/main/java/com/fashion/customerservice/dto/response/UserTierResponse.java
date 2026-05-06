package com.fashion.customerservice.dto.response;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class UserTierResponse {
    private String code;
    private String name;
    private BigDecimal discountPercent;
}