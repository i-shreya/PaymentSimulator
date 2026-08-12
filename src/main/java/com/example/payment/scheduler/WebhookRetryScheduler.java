package com.example.payment.scheduler;

import com.example.payment.webhook.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebhookRetryScheduler {
    private final WebhookService webhooks;

    @Scheduled(fixedDelay = 1000)
    void retry() {
        webhooks.deliverDue();
    }
}
