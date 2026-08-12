package com.example.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String entityType;

    @Column(nullable = false)
    private String entityId;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(length = 30)
    private String oldStatus;

    @Column(length = 30)
    private String newStatus;

    @Column(length = 120)
    private String performedBy;

    @Column(columnDefinition = "text")
    private String metadata;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
