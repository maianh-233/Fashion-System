package com.fashionsystem.fashion_system.dto.employee;

import java.util.UUID;

public record EligibleSubordinateResponse(
        UUID id,
        String employeeCode,
        String fullName,
        String email,
        UUID positionId,
        String positionName,
        Integer hierarchyLevel) {}
