package com.fashion.authservice.entity;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "permission_groups")
@Getter @Setter
public class PermissionGroup {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    @Column(unique = true)
    private String code;

    private String description;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "group")
    private Set<Permission> permissions = new HashSet<>();
}