package com.example.payment;

import com.example.payment.entity.PaymentMethod;
import com.example.payment.payment.PaymentSimulator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentSimulatorTest {
    private final PaymentSimulator simulator = new PaymentSimulator();

    @Test
    void deterministicCardTokens() {
        assertTrue(simulator.simulate(PaymentMethod.CARD, "4242").success());
        assertFalse(simulator.simulate(PaymentMethod.CARD, "4000").success());
        assertTrue(simulator.simulate(PaymentMethod.CARD, "4111").retryable());
    }

    @Test
    void deterministicUpiTokens() {
        assertTrue(simulator.simulate(PaymentMethod.UPI, "success@upi").success());
        assertFalse(simulator.simulate(PaymentMethod.UPI, "fail@upi").success());
    }
}
