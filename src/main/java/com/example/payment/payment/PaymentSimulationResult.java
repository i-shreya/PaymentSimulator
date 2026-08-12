package com.example.payment.payment;

public record PaymentSimulationResult(boolean success, boolean retryable, String failureReason) {}
