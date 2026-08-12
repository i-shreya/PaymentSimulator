package com.example.payment.payment;

import com.example.payment.entity.PaymentStatus;
import com.example.payment.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class PaymentStateTransitionService {
    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED = Map.of(
            PaymentStatus.PENDING, Set.of(PaymentStatus.PROCESSING, PaymentStatus.CANCELLED),
            PaymentStatus.PROCESSING, Set.of(PaymentStatus.SUCCEEDED, PaymentStatus.FAILED),
            PaymentStatus.SUCCEEDED, Set.of(PaymentStatus.REFUND_PENDING, PaymentStatus.PARTIALLY_REFUNDED),
            PaymentStatus.REFUND_PENDING, Set.of(PaymentStatus.REFUNDED, PaymentStatus.PARTIALLY_REFUNDED)
    );

    public void validate(PaymentStatus current, PaymentStatus next) {
        if (!ALLOWED.getOrDefault(current, Set.of()).contains(next)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_PAYMENT_STATE",
                    "Cannot transition payment from " + current + " to " + next + ".");
        }
    }
}
