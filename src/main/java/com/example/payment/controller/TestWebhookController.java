package com.example.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test-webhooks")
public class TestWebhookController {
    @Operation(summary = "Fake merchant webhook receiver", description = "Receives simulated webhook payloads inside the same application.")
    @PostMapping("/receive")
    ResponseEntity<Map<String, String>> receive(@RequestBody(required = false) Map<String, Object> payload,
                                                @RequestParam(defaultValue = "false") boolean fail) {
        if (fail) return ResponseEntity.status(503).body(Map.of("status", "simulated-failure"));
        return ResponseEntity.ok(Map.of("status", "received"));
    }
}
