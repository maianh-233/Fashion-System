package com.fashion.orderservice.dto.request;

import com.fashion.orderservice.entity.enums.AddressType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderAddressRequest {

    private String receiverName;

    private String receiverPhone;

    private String province;

    private String district;

    private String ward;

    private String addressLine;

    private String postalCode;

    private AddressType addressType;
}
