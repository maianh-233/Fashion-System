package com.fashionsystem.fashion_system.exception;

import com.fashionsystem.fashion_system.dto.hierarchy.HierarchyImpactResponse;
import org.springframework.http.HttpStatus;

public class HierarchyConfirmationRequiredException extends BusinessException {
    public HierarchyConfirmationRequiredException(HierarchyImpactResponse impact) {
        super(HttpStatus.CONFLICT, impact.summary());
        getBody().setProperty("code", "HIERARCHY_CONFIRMATION_REQUIRED");
        getBody().setProperty("affectedRelationCount", impact.affectedRelationCount());
        getBody().setProperty("affectedEmployeeCount", impact.affectedEmployeeCount());
    }
}
