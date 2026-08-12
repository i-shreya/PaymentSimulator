package com.example.payment.mapper;

import com.example.payment.dto.EventDtos.*;
import com.example.payment.dto.PaymentDtos.*;
import com.example.payment.entity.*;

public final class PaymentMapper {
    private PaymentMapper() {}

    public static PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(p.getPublicId(), p.getPaymentReference(), p.getAmount(), p.getCurrency(), p.getDescription(),
                p.getStatus(), p.getPaymentMethod(), p.getFailureReason(), p.getCreatedAt(), p.getCompletedAt());
    }

    public static RefundResponse toResponse(Refund r) {
        return new RefundResponse(r.getPublicId(), r.getRefundReference(), r.getAmount(), r.getStatus(), r.getReason(), r.getCreatedAt(), r.getCompletedAt());
    }

    public static WebhookResponse toResponse(WebhookEvent e) {
        return new WebhookResponse(e.getEventId(), e.getEventType(), e.getPayload(), e.getStatus(), e.getAttemptCount(), e.getNextRetryAt(), e.getDeliveredAt());
    }

    public static AuditResponse toResponse(AuditLog a) {
        return new AuditResponse(a.getEntityType(), a.getEntityId(), a.getAction(), a.getOldStatus(), a.getNewStatus(), a.getPerformedBy(), a.getMetadata(), a.getCreatedAt());
    }
}
