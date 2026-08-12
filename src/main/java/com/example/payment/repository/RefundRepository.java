package com.example.payment.repository;

import com.example.payment.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByPaymentPublicIdOrderByCreatedAtDesc(UUID paymentId);

    @Query("select coalesce(sum(r.amount), 0) from Refund r where r.payment = :payment and r.status = 'SUCCEEDED'")
    BigDecimal succeededAmount(@Param("payment") Payment payment);

    @Query(value = """
        select coalesce(sum(r.amount), 0)
        from refunds r
        join payments p on p.id = r.payment_id
        where r.status = 'SUCCEEDED' and (:merchantId is null or p.merchant_id = :merchantId)
        """, nativeQuery = true)
    BigDecimal aggregateRefunded(@Param("merchantId") Long merchantId);
}
