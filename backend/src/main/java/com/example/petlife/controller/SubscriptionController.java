package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.common.PageResponse;
import com.example.petlife.dto.subscription.RenewalHistoryRow;
import com.example.petlife.dto.subscription.SubscriptionRow;
import com.example.petlife.entity.NotificationEntity;
import com.example.petlife.mapper.NotificationMapper;
import com.example.petlife.mapper.SubscriptionMapper;
import com.example.petlife.mapper.UserMapper;
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

    private final SubscriptionMapper subscriptionMapper;
    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    public SubscriptionController(SubscriptionMapper subscriptionMapper,
                                  NotificationMapper notificationMapper,
                                  UserMapper userMapper) {
        this.subscriptionMapper = subscriptionMapper;
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
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

        ra.addFlashAttribute("success", "更新申請を送信しました");
        return "redirect:/app/subscriptions";
    }
}
