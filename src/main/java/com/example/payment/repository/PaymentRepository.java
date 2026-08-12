package com.example.payment.repository;

import com.example.payment.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    Optional<Payment> findByPublicId(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.publicId = :publicId")
    Optional<Payment> findByPublicIdForUpdate(@Param("publicId") UUID publicId);

    @Modifying
    @Query("update Payment p set p.status = :next, p.updatedAt = :now where p.id = :id and p.status = :expected")
    int transitionIfStatus(@Param("id") Long id, @Param("expected") PaymentStatus expected, @Param("next") PaymentStatus next, @Param("now") Instant now);

    @Query(value = """
        select count(*) as total_payments,
               coalesce(sum(case when status = 'SUCCEEDED' then 1 else 0 end), 0) as successful_payments,
               coalesce(sum(case when status = 'FAILED' then 1 else 0 end), 0) as failed_payments,
               coalesce(sum(case when status = 'PENDING' then 1 else 0 end), 0) as pending_payments,
               coalesce(sum(case when status in ('SUCCEEDED','PARTIALLY_REFUNDED','REFUND_PENDING') then amount else 0 end), 0) as total_successful_amount
        from payments
        where (:merchantId is null or merchant_id = :merchantId)
        """, nativeQuery = true)
    Object[] aggregateStats(@Param("merchantId") Long merchantId);

    static Specification<Payment> filters(User merchant, PaymentStatus status, CurrencyCode currency, Instant from, Instant to, BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (merchant != null) predicate = cb.and(predicate, cb.equal(root.get("merchant"), merchant));
            if (status != null) predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            if (currency != null) predicate = cb.and(predicate, cb.equal(root.get("currency"), currency));
            if (from != null) predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("createdAt"), to));
            if (min != null) predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("amount"), min));
            if (max != null) predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("amount"), max));
            return predicate;
        };
    }
}
