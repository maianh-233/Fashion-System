package com.fashion.customerservice.dto.response;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class LoyaltyResponse {
    private BigDecimal totalSpent;
    private Integer pointsBalance;
}
