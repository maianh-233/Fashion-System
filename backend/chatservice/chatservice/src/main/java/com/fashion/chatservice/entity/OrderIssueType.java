package com.fashion.chatservice.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_issue_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderIssueType {

    @Id
    private String code;

    @Column(nullable = false)
    private String name;
}