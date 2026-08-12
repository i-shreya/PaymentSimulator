package com.example.payment;

import com.example.payment.entity.PaymentStatus;
import com.example.payment.exception.ApiException;
import com.example.payment.payment.PaymentStateTransitionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentStateTransitionServiceTest {
    private final PaymentStateTransitionService service = new PaymentStateTransitionService();

    @Test
    void allowsPendingToProcessing() {
        assertDoesNotThrow(() -> service.validate(PaymentStatus.PENDING, PaymentStatus.PROCESSING));
    }

    @Test
    void rejectsArbitraryTransition() {
        assertThrows(ApiException.class, () -> service.validate(PaymentStatus.FAILED, PaymentStatus.SUCCEEDED));
    }
}
