package com.fashion.customerservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fashion.customerservice.constant.ActionType;
import com.fashion.customerservice.constant.EntityType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "customer_activity_logs")
@Getter 
@Setter
public class CustomerActivityLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private ActionType action;

    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    private UUID entityId;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}