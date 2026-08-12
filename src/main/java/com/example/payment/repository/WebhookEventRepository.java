package com.example.payment.repository;

import com.example.payment.entity.WebhookEvent;
import com.example.payment.entity.WebhookStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    List<WebhookEvent> findByPaymentPublicIdOrderByCreatedAtDesc(UUID paymentId);
    List<WebhookEvent> findTop25ByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(WebhookStatus status, Instant now);
}
