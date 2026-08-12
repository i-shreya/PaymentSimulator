package com.example.payment.controller;

import com.example.payment.dto.EventDtos.*;
import com.example.payment.dto.PaymentDtos.*;
import com.example.payment.entity.*;
import com.example.payment.mapper.PaymentMapper;
import com.example.payment.repository.*;
import com.example.payment.security.AppUserDetails;
import com.example.payment.service.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService payments;
    private final PaymentProcessingService processing;
    private final RefundService refunds;
    private final StatisticsService statistics;
    private final WebhookEventRepository webhookEvents;
    private final AuditLogRepository auditLogs;

    @Operation(summary = "Create payment", description = "Creates a pending payment using a required Idempotency-Key header.")
    @PostMapping
    ResponseEntity<String> create(@Valid @RequestBody CreatePaymentRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey,
                                  @AuthenticationPrincipal AppUserDetails principal) {
        return payments.create(request, principal.user(), idempotencyKey);
    }

    @Operation(summary = "Get payment", description = "Returns one payment visible to the current merchant or admin.")
    @GetMapping("/{id}")
    PaymentResponse get(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        return payments.get(id, principal.user());
    }

    @Operation(summary = "List payments", description = "Supports pagination, sorting, and filters by status, currency, date, and amount.")
    @GetMapping
    Page<PaymentResponse> list(@RequestParam(required = false) PaymentStatus status,
                               @RequestParam(required = false) CurrencyCode currency,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                               @RequestParam(required = false) BigDecimal minAmount,
                               @RequestParam(required = false) BigDecimal maxAmount,
                               Pageable pageable,
                               @AuthenticationPrincipal AppUserDetails principal) {
        return payments.list(principal.user(), status, currency, from, to, minAmount, maxAmount, pageable);
    }

    @Operation(summary = "Process payment", description = "Atomically moves PENDING to PROCESSING and completes asynchronously.")
    @PostMapping("/{id}/process")
    PaymentResponse process(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        processing.start(id, principal.user().getEmail());
        processing.completeLater(id);
        return payments.get(id, principal.user());
    }

    @Operation(summary = "Cancel payment", description = "Cancels a pending payment.")
    @PostMapping("/{id}/cancel")
    PaymentResponse cancel(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        return payments.cancel(id, principal.user());
    }

    @Operation(summary = "Refund payment", description = "Creates a full or partial refund while preventing over-refunds under concurrency.")
    @PostMapping("/{id}/refund")
    RefundResponse refund(@PathVariable UUID id, @Valid @RequestBody RefundRequest request, @AuthenticationPrincipal AppUserDetails principal) {
        return refunds.refund(id, request, principal.user());
    }

    @Operation(summary = "List refunds", description = "Returns refunds for a payment.")
    @GetMapping("/{id}/refunds")
    List<RefundResponse> listRefunds(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        return refunds.list(id, principal.user());
    }

    @Operation(summary = "List webhook events", description = "Returns simulated webhook events for a payment.")
    @GetMapping("/{id}/events")
    List<WebhookResponse> events(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        payments.owned(id, principal.user());
        return webhookEvents.findByPaymentPublicIdOrderByCreatedAtDesc(id).stream().map(PaymentMapper::toResponse).toList();
    }

    @Operation(summary = "List audit logs", description = "Returns append-only audit events for a payment.")
    @GetMapping("/{id}/audit-logs")
    List<AuditResponse> audits(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        payments.owned(id, principal.user());
        return auditLogs.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("PAYMENT", id.toString()).stream().map(PaymentMapper::toResponse).toList();
    }

    @Operation(summary = "Payment statistics", description = "Returns aggregate payment counters and amounts.")
    @GetMapping("/statistics")
    StatisticsResponse stats(@AuthenticationPrincipal AppUserDetails principal) {
        return statistics.stats(principal.user());
    }
}
