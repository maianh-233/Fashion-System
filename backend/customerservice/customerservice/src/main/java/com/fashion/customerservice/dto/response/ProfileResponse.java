package com.fashion.customerservice.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import com.fashion.customerservice.constant.Gender;

import lombok.Getter;
import lombok.Setter;
@Getter 
@Setter
public class ProfileResponse {
    private UUID userId;
    private String fullName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String avatar;
}
