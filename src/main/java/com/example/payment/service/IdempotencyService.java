package com.example.payment.service;

import com.example.payment.entity.*;
import com.example.payment.exception.ApiException;
import com.example.payment.repository.IdempotencyKeyRepository;
import com.example.payment.util.Hashing;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final IdempotencyKeyRepository keys;
    private final ObjectMapper objectMapper;

    public String hash(Object body) {
        try {
            return Hashing.sha256(objectMapper.writeValueAsString(body));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Optional<IdempotencyKey> find(User merchant, String key) {
        return keys.findByMerchantAndKey(merchant, key);
    }

    public void store(User merchant, String key, String requestHash, String payload, int status) {
        var idem = new IdempotencyKey();
        idem.setMerchant(merchant);
        idem.setKey(key);
        idem.setRequestHash(requestHash);
        idem.setResponsePayload(payload);
        idem.setResponseStatus(status);
        keys.saveAndFlush(idem);
    }

    public ResponseEntity<String> replay(IdempotencyKey key, String requestHash) {
        if (!key.getRequestHash().equals(requestHash)) {
            throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "Idempotency key was reused with a different request body.");
        }
        return ResponseEntity.status(key.getResponseStatus()).contentType(MediaType.APPLICATION_JSON).body(key.getResponsePayload());
    }
}
