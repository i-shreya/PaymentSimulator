package com.example.payment.service;

import com.example.payment.audit.AuditService;
import com.example.payment.entity.PaymentStatus;
import com.example.payment.exception.ApiException;
import com.example.payment.payment.*;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.webhook.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PaymentProcessingService {
    private final PaymentRepository payments;
    private final PaymentStateTransitionService transitions;
    private final PaymentSimulator simulator;
    private final AuditService audit;
    private final WebhookService webhooks;
    @Value("${app.payment.processing-delay-min-ms}") private long minDelay;
    @Value("${app.payment.processing-delay-max-ms}") private long maxDelay;

    @Transactional
    public void start(UUID paymentId, String actor) {
        var payment = payments.findByPublicId(paymentId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found."));
        transitions.validate(payment.getStatus(), PaymentStatus.PROCESSING);
        // Atomic conditional update lets concurrent /process calls race safely without holding a long database lock.
        int changed = payments.transitionIfStatus(payment.getId(), PaymentStatus.PENDING, PaymentStatus.PROCESSING, Instant.now());
        if (changed != 1) throw new ApiException(HttpStatus.CONFLICT, "PAYMENT_ALREADY_PROCESSING", "Payment was already processed or is no longer pending.");
        audit.record("PAYMENT", paymentId.toString(), "PAYMENT_PROCESSING", PaymentStatus.PENDING.name(), PaymentStatus.PROCESSING.name(), actor, null);
        payment.setStatus(PaymentStatus.PROCESSING);
        webhooks.create(payment, "payment.processing");
    }

    @Async
    public void completeLater(UUID paymentId) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(minDelay, maxDelay + 1));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        complete(paymentId);
    }

    @Transactional
    public void complete(UUID paymentId) {
        var payment = payments.findByPublicId(paymentId).orElseThrow();
        if (payment.getStatus() != PaymentStatus.PROCESSING) return;
        var result = simulator.simulate(payment.getPaymentMethod(), payment.getSimulationToken());
        var old = payment.getStatus();
        payment.setStatus(result.success() ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED);
        payment.setFailureReason(result.failureReason());
        payment.setCompletedAt(Instant.now());
        audit.record("PAYMENT", paymentId.toString(), result.success() ? "PAYMENT_SUCCEEDED" : "PAYMENT_FAILED", old.name(), payment.getStatus().name(), "system", result.retryable() ? "retryable=true" : null);
        webhooks.create(payment, result.success() ? "payment.succeeded" : "payment.failed");
    }
}
