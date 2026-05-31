package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.billing.InvoiceRow;
import com.example.petlife.dto.common.PageResponse;
import com.example.petlife.dto.subscription.RenewalHistoryRow;
import com.example.petlife.dto.subscription.SubscriptionRow;
import com.example.petlife.entity.NotificationEntity;
import com.example.petlife.mapper.NotificationMapper;
import com.example.petlife.mapper.SubscriptionMapper;
import com.example.petlife.mapper.UserMapper;
import com.example.petlife.service.BillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/app/subscriptions")
public class SubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionMapper subscriptionMapper;
    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final BillingService billingService;

    public SubscriptionController(SubscriptionMapper subscriptionMapper,
                                  NotificationMapper notificationMapper,
                                  UserMapper userMapper,
                                  BillingService billingService) {
        this.subscriptionMapper = subscriptionMapper;
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
        this.billingService = billingService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model,
                       @AuthenticationPrincipal LoginUser currentUser) {
        int offset = (page - 1) * size;
        List<SubscriptionRow> items;
        long total;
        if (currentUser.isAdmin()) {
            items = subscriptionMapper.findAllRows(size, offset);
            total = subscriptionMapper.countAll();
        } else {
            items = subscriptionMapper.findRowsByUserId(currentUser.id(), size, offset);
            total = subscriptionMapper.countByUserId(currentUser.id());
            Set<Long> pendingRenewalIds = new HashSet<>(
                    notificationMapper.findRenewalRequestedSubscriptionIdsByUserId(currentUser.id()));
            List<RenewalHistoryRow> renewalHistory =
                    notificationMapper.findRenewalHistoryByUserId(currentUser.id());
            model.addAttribute("pendingRenewalIds", pendingRenewalIds);
            model.addAttribute("renewalHistory", renewalHistory);
        }
        model.addAttribute("page", new PageResponse<>(items, page, size, total));
        model.addAttribute("isAdminView", currentUser.isAdmin());
        return "subscriptions/index";
    }

    @PostMapping("/{id}/renew-request")
    public String renewRequest(@PathVariable Long id,
                               @AuthenticationPrincipal LoginUser currentUser,
                               RedirectAttributes ra) {
        SubscriptionRow target = subscriptionMapper.findRowByIdAndUserId(id, currentUser.id());
        if (target == null) {
            ra.addFlashAttribute("error", "該当するサブスクリプションが見つかりません");
            return "redirect:/app/subscriptions";
        }

        String title = "サブスクリプション更新申請 #" + id;
        Set<Long> already = new HashSet<>(
                notificationMapper.findRenewalRequestedSubscriptionIdsByUserId(currentUser.id()));
        if (already.contains(id)) {
            ra.addFlashAttribute("error", "この契約はすでに更新申請済みです");
            return "redirect:/app/subscriptions";
        }

        String endDate = target.endDate() != null ? target.endDate().toString() : "未設定";
        String body = currentUser.displayName() + " さんより「" + target.planName()
                + "」プラン（終了日: " + endDate + "）の更新申請がありました。";

        NotificationEntity notification = new NotificationEntity(
                null, "INFO", title, body,
                null, LocalDateTime.now(), "SENT",
                currentUser.id(), null, null, null
        );
        Long notificationId = notificationMapper.insertReturningId(notification);
        List<Long> adminIds = userMapper.findAdminUserIds();
        for (Long adminId : adminIds) {
            notificationMapper.insertRecipient(notificationId, adminId);
            notificationMapper.updateRecipientStatus(notificationId, adminId, "SENT");
        }

        // 請求書を発行し、アプリ内通知（同期）+ メール・LINE（非同期）で顧客へ案内
        try {
            InvoiceRow invoice = billingService.createInvoice(id);
            log.info("Invoice created for subscription {}: {}", id, invoice.invoiceNumber());
        } catch (Exception e) {
            log.error("Failed to create invoice for subscription {}: {}", id, e.getMessage(), e);
            ra.addFlashAttribute("error", "請求書の発行に失敗しました: " + e.getMessage());
            return "redirect:/app/subscriptions";
        }

        ra.addFlashAttribute("success", "更新申請を送信しました。お支払いのご案内を通知センターでご確認ください。");
        return "redirect:/app/subscriptions";
    }
}
