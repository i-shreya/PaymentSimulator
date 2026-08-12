package com.example.payment.dto;

import com.example.payment.entity.WebhookStatus;

import java.time.Instant;
import java.util.UUID;

public final class EventDtos {
    public record WebhookResponse(UUID eventId, String eventType, String payload, WebhookStatus status, int attemptCount, Instant nextRetryAt, Instant deliveredAt) {}
    public record AuditResponse(String entityType, String entityId, String action, String oldStatus, String newStatus, String performedBy, String metadata, Instant createdAt) {}
}
