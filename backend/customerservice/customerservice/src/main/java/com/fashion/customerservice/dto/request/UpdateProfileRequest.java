package com.fashion.customerservice.dto.request;

import java.time.LocalDate;

import com.fashion.customerservice.constant.Gender;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class UpdateProfileRequest {
    private String fullName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String avatar;
}
