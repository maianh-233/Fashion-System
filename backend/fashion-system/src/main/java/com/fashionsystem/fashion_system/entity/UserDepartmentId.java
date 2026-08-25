package com.fashionsystem.fashion_system.entity;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Khóa kép của UserDepartment. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class UserDepartmentId implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID userId;
    private UUID departmentId;
}
