package com.example.petlife.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OverdueInvoiceScheduler {

    private final BillingService billingService;

    public OverdueInvoiceScheduler(BillingService billingService) {
        this.billingService = billingService;
    }

    /** 毎日 02:00（サーバーローカル時刻）に期限超過請求を処理する。 */
    @Scheduled(cron = "0 0 2 * * *")
    public void processOverdueInvoicesDaily() {
        billingService.processOverdueInvoices();
    }
}
