package com.example.payment.dto;

import com.example.payment.entity.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class PaymentDtos {
    public record CreatePaymentRequest(
            @NotNull @DecimalMin("0.01") @DecimalMax("10000000.00") BigDecimal amount,
            @NotNull CurrencyCode currency,
            @Size(max = 500) String description,
            @NotNull PaymentMethod paymentMethod,
            @Size(max = 120) String simulationToken) {}

    public record PaymentResponse(UUID paymentId, String paymentReference, BigDecimal amount, CurrencyCode currency,
                                  String description, PaymentStatus status, PaymentMethod paymentMethod,
                                  String failureReason, Instant createdAt, Instant completedAt) {}

    public record RefundRequest(@NotNull @DecimalMin("0.01") BigDecimal amount, @Size(max = 300) String reason) {}
    public record RefundResponse(UUID refundId, String refundReference, BigDecimal amount, RefundStatus status, String reason, Instant createdAt, Instant completedAt) {}
    public record StatisticsResponse(long totalPayments, long successfulPayments, long failedPayments, long pendingPayments, BigDecimal totalSuccessfulAmount, BigDecimal totalRefundedAmount) {}
}
