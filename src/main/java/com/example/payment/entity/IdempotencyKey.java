package com.example.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "idempotency_keys", uniqueConstraints = @UniqueConstraint(columnNames = {"merchant_id", "idempotency_key"}))
public class IdempotencyKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private User merchant;

    @Column(name = "idempotency_key", nullable = false, length = 180)
    private String key;

    @Column(nullable = false, length = 128)
    private String requestHash;

    @Column(nullable = false, columnDefinition = "text")
    private String responsePayload;

    @Column(nullable = false)
    private int responseStatus;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
