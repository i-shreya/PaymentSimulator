package com.example.payment.service;

import com.example.payment.audit.AuditService;
import com.example.payment.dto.PaymentDtos.*;
import com.example.payment.entity.*;
import com.example.payment.exception.ApiException;
import com.example.payment.mapper.PaymentMapper;
import com.example.payment.payment.PaymentStateTransitionService;
import com.example.payment.repository.*;
import com.example.payment.webhook.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundService {
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final PaymentService paymentService;
    private final AuditService audit;
    private final WebhookService webhooks;

    @Transactional
    public RefundResponse refund(UUID paymentId, RefundRequest request, User actor) {
        paymentService.owned(paymentId, actor);
        var payment = payments.findByPublicIdForUpdate(paymentId).orElseThrow();
        if (payment.getStatus() != PaymentStatus.SUCCEEDED && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_REFUND_STATE", "Only succeeded payments can be refunded.");
        }
        BigDecimal refunded = refunds.succeededAmount(payment);
        BigDecimal remaining = payment.getAmount().subtract(refunded);
        if (request.amount().compareTo(remaining) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_REFUNDABLE_AMOUNT", "Refund amount exceeds remaining refundable amount.");
        }
        var old = payment.getStatus();
        payment.setStatus(PaymentStatus.REFUND_PENDING);
        var refund = new Refund();
        refund.setPayment(payment);
        refund.setRefundReference("rf_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        refund.setAmount(request.amount());
        refund.setReason(request.reason());
        refund.setStatus(RefundStatus.SUCCEEDED);
        refund.setCompletedAt(Instant.now());
        refunds.save(refund);
        BigDecimal after = refunded.add(request.amount());
        payment.setStatus(after.compareTo(payment.getAmount()) == 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        audit.record("PAYMENT", paymentId.toString(), "REFUND_SUCCEEDED", old.name(), payment.getStatus().name(), actor.getEmail(), "amount=" + request.amount());
        webhooks.create(payment, "refund.succeeded");
        webhooks.create(payment, payment.getStatus() == PaymentStatus.REFUNDED ? "payment.refunded" : "payment.partially_refunded");
        return PaymentMapper.toResponse(refund);
    }

    public List<RefundResponse> list(UUID paymentId, User actor) {
        paymentService.owned(paymentId, actor);
        return refunds.findByPaymentPublicIdOrderByCreatedAtDesc(paymentId).stream().map(PaymentMapper::toResponse).toList();
    }
}
