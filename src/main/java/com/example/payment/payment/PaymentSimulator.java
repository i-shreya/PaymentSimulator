package com.example.payment.payment;

import com.example.payment.entity.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class PaymentSimulator {
    public PaymentSimulationResult simulate(PaymentMethod method, String token) {
        if (method == PaymentMethod.CARD) {
            if (token != null && token.endsWith("4242")) return new PaymentSimulationResult(true, false, null);
            if (token != null && token.endsWith("4000")) return new PaymentSimulationResult(false, false, "Card declined by simulator.");
            if (token != null && token.endsWith("4111")) return new PaymentSimulationResult(false, true, "Temporary gateway failure.");
        }
        if (method == PaymentMethod.UPI) {
            if ("success@upi".equalsIgnoreCase(token)) return new PaymentSimulationResult(true, false, null);
            if ("fail@upi".equalsIgnoreCase(token)) return new PaymentSimulationResult(false, false, "UPI payment failed by simulator.");
        }
        return new PaymentSimulationResult(true, false, null);
    }
}
