package com.example.payment.webhook;

import com.example.payment.audit.AuditService;
import com.example.payment.entity.*;
import com.example.payment.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class WebhookService {
    private final WebhookEventRepository events;
    private final AuditService audit;
    @Value("${app.webhook.max-retries}") private int maxRetries;

    @Transactional
    public void create(Payment payment, String type) {
        var event = new WebhookEvent();
        event.setPayment(payment);
        event.setEventType(type);
        event.setPayload("{\"eventType\":\"" + type + "\",\"paymentId\":\"" + payment.getPublicId() + "\",\"status\":\"" + payment.getStatus() + "\"}");
        event.setNextRetryAt(Instant.now());
        events.save(event);
    }

    @Transactional
    public void deliverDue() {
        for (var event : events.findTop25ByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(WebhookStatus.PENDING, Instant.now())) {
            deliver(event);
        }
    }

    public void deliver(WebhookEvent event) {
        event.setAttemptCount(event.getAttemptCount() + 1);
        boolean success = event.getAttemptCount() >= 3 || !event.getEventType().contains("failed");
        if (success) {
            event.setStatus(WebhookStatus.DELIVERED);
            event.setDeliveredAt(Instant.now());
            audit.record("PAYMENT", event.getPayment().getPublicId().toString(), "WEBHOOK_SENT", null, null, "system", event.getEventType());
            return;
        }
        if (event.getAttemptCount() >= maxRetries) {
            event.setStatus(WebhookStatus.FAILED);
            audit.record("PAYMENT", event.getPayment().getPublicId().toString(), "WEBHOOK_FAILED", null, null, "system", event.getEventType());
            return;
        }
        long backoffSeconds = (long) Math.pow(2, event.getAttemptCount() - 1);
        event.setNextRetryAt(Instant.now().plusSeconds(backoffSeconds));
        audit.record("PAYMENT", event.getPayment().getPublicId().toString(), "WEBHOOK_FAILED", null, null, "system", "attempt=" + event.getAttemptCount());
    }
}
