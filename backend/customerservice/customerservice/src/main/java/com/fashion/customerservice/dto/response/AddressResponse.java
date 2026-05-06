package com.fashion.customerservice.dto.response;

import java.util.UUID;

import com.fashion.customerservice.constant.AddressType;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class AddressResponse {
    private UUID id;

    private String receiverName;
    private String receiverPhone;

    private String province;
    private String district;
    private String ward;

    private String addressLine;
    private String postalCode;

    private Boolean isDefault;
    private AddressType addressType;
}
