package com.example.payment.service;

import com.example.payment.audit.AuditService;
import com.example.payment.dto.PaymentDtos.*;
import com.example.payment.entity.*;
import com.example.payment.exception.ApiException;
import com.example.payment.mapper.PaymentMapper;
import com.example.payment.payment.PaymentStateTransitionService;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.webhook.WebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository payments;
    private final IdempotencyService idempotency;
    private final PaymentStateTransitionService transitions;
    private final AuditService audit;
    private final WebhookService webhooks;
    private final ObjectMapper objectMapper;

    @Transactional
    public ResponseEntity<String> create(CreatePaymentRequest request, User merchant, String key) {
        if (key == null || key.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key header is required.");
        String requestHash = idempotency.hash(request);
        var existing = idempotency.find(merchant, key);
        if (existing.isPresent()) return idempotency.replay(existing.get(), requestHash);

        var payment = new Payment();
        payment.setMerchant(merchant);
        payment.setPaymentReference("pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency());
        payment.setDescription(request.description());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setSimulationToken(safeToken(request));
        payments.save(payment);
        audit.record("PAYMENT", payment.getPublicId().toString(), "PAYMENT_CREATED", null, PaymentStatus.PENDING.name(), merchant.getEmail(), null);
        webhooks.create(payment, "payment.created");
        try {
            String payload = objectMapper.writeValueAsString(PaymentMapper.toResponse(payment));
            idempotency.store(merchant, key, requestHash, payload, HttpStatus.CREATED.value());
            return ResponseEntity.status(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(payload);
        } catch (DataIntegrityViolationException ex) {
            var raced = idempotency.find(merchant, key).orElseThrow(() -> ex);
            return idempotency.replay(raced, requestHash);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String safeToken(CreatePaymentRequest request) {
        if (request.simulationToken() == null) return null;
        return request.paymentMethod() == PaymentMethod.CARD && request.simulationToken().length() > 4
                ? "****" + request.simulationToken().substring(request.simulationToken().length() - 4)
                : request.simulationToken();
    }

    public PaymentResponse get(UUID id, User actor) {
        var payment = owned(id, actor);
        return PaymentMapper.toResponse(payment);
    }

    public Page<PaymentResponse> list(User actor, PaymentStatus status, CurrencyCode currency, Instant from, Instant to, BigDecimal min, BigDecimal max, Pageable pageable) {
        User merchant = actor.getRole() == UserRole.ADMIN ? null : actor;
        return payments.findAll(PaymentRepository.filters(merchant, status, currency, from, to, min, max), pageable).map(PaymentMapper::toResponse);
    }

    @Transactional
    public PaymentResponse cancel(UUID id, User actor) {
        var payment = owned(id, actor);
        transitions.validate(payment.getStatus(), PaymentStatus.CANCELLED);
        var old = payment.getStatus();
        payment.setStatus(PaymentStatus.CANCELLED);
        audit.record("PAYMENT", id.toString(), "PAYMENT_CANCELLED", old.name(), PaymentStatus.CANCELLED.name(), actor.getEmail(), null);
        return PaymentMapper.toResponse(payment);
    }

    public Payment owned(UUID id, User actor) {
        var payment = payments.findByPublicId(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found."));
        if (actor.getRole() != UserRole.ADMIN && !payment.getMerchant().getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "PAYMENT_FORBIDDEN", "Payment belongs to another merchant.");
        }
        return payment;
    }
}
