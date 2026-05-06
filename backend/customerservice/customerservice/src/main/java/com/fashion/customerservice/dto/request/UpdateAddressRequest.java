package com.fashion.customerservice.dto.request;

import com.fashion.customerservice.constant.AddressType;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class UpdateAddressRequest {
    private String receiverName;
    private String receiverPhone;

    private String province;
    private String district;
    private String ward;

    private String addressLine;
    private String postalCode;

    private AddressType addressType;
}
