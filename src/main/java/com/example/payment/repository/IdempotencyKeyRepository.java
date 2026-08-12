package com.example.payment.repository;

import com.example.payment.entity.IdempotencyKey;
import com.example.payment.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    Optional<IdempotencyKey> findByMerchantAndKey(User merchant, String key);
}
