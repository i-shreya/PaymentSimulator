package com.example.payment.service;

import com.example.payment.dto.PaymentDtos.StatisticsResponse;
import com.example.payment.entity.*;
import com.example.payment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final PaymentRepository payments;
    private final RefundRepository refunds;

    public StatisticsResponse stats(User actor) {
        Long merchantId = actor.getRole() == UserRole.ADMIN ? null : actor.getId();
        Object[] values = payments.aggregateStats(merchantId);
        BigDecimal refunded = refunds.aggregateRefunded(merchantId);
        return new StatisticsResponse(((Number) values[0]).longValue(), ((Number) values[1]).longValue(),
                ((Number) values[2]).longValue(), ((Number) values[3]).longValue(), (BigDecimal) values[4], refunded);
    }
}
