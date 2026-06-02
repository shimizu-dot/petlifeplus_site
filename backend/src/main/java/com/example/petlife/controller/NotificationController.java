package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.notification.NotificationManageForm;
import com.example.petlife.entity.PetCareRecordEntity;
import com.example.petlife.entity.NotificationEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.mapper.AppointmentMapper;
import com.example.petlife.mapper.DismissedReminderMapper;
import com.example.petlife.mapper.NotificationMapper;
import com.example.petlife.mapper.SubscriptionMapper;
import com.example.petlife.service.PetCareRecordService;
import com.example.petlife.service.PetService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/app/notifications")
public class NotificationController {

    private final NotificationMapper notificationMapper;
    private final PetService petService;
    private final PetCareRecordService petCareRecordService;
    private final AppointmentMapper appointmentMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final DismissedReminderMapper dismissedReminderMapper;
    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private static final Set<String> ALLOWED_TYPES = Set.of("REMINDER", "INFO", "ALERT");
    private static final Set<String> ALLOWED_SCOPES = Set.of("ALL", "USER", "STAFF", "VET", "ADMIN");

    public NotificationController(NotificationMapper notificationMapper,
                                  PetService petService,
                                  PetCareRecordService petCareRecordService,
                                  AppointmentMapper appointmentMapper,
                                  SubscriptionMapper subscriptionMapper,
                                  DismissedReminderMapper dismissedReminderMapper) {
        this.notificationMapper = notificationMapper;
        this.petService = petService;
        this.petCareRecordService = petCareRecordService;
        this.appointmentMapper = appointmentMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.dismissedReminderMapper = dismissedReminderMapper;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model,
                       @AuthenticationPrincipal LoginUser currentUser) {
        int offset = (page - 1) * size;
        Set<String> dismissed = dismissedReminderMapper.findKeysByUserId(currentUser.id());
        model.addAttribute("scheduleReminders", buildScheduleReminders(currentUser, dismissed));
        model.addAttribute("notifications",
                notificationMapper.findByUserId(currentUser.id(), size, offset));
        model.addAttribute("total", notificationMapper.countByUserId(currentUser.id()));
        model.addAttribute("unreadCount", notificationMapper.countUnreadByUserId(currentUser.id()));
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        return "notifications/index";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable Long id,
                             @AuthenticationPrincipal LoginUser currentUser,
                             RedirectAttributes ra) {
        notificationMapper.markAsRead(id, currentUser.id());
        return "redirect:/app/notifications";
    }

    @PostMapping("/read-all")
    public String markAllAsRead(@AuthenticationPrincipal LoginUser currentUser,
                                RedirectAttributes ra) {
        notificationMapper.markAllAsRead(currentUser.id());
        ra.addFlashAttribute("success", "すべての通知を既読にしました");
        return "redirect:/app/notifications";
    }

    @PostMapping("/reminders/dismiss")
    public String dismissReminder(@RequestParam String reminderKey,
                                  @AuthenticationPrincipal LoginUser currentUser,
                                  RedirectAttributes ra) {
        dismissedReminderMapper.insert(currentUser.id(), reminderKey);
        ra.addFlashAttribute("success", "リマインダーを確認しました");
        return "redirect:/app/notifications";
    }

    @GetMapping("/manage")
    public String manage(@RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "20") int size,
                         Model model,
                         @AuthenticationPrincipal LoginUser currentUser) {
        ensureCanManageNotifications(currentUser);
        if (!model.containsAttribute("form")) {
            NotificationManageForm form = new NotificationManageForm();
            form.setNotificationType("INFO");
            form.setTargetScope("ALL");
            model.addAttribute("form", form);
        }
        int offset = Math.max(page - 1, 0) * size;
        model.addAttribute("rows", notificationMapper.findCreatedByUserId(currentUser.id(), size, offset));
        model.addAttribute("total", notificationMapper.countCreatedByUserId(currentUser.id()));
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        return "notifications/manage";
    }

    @PostMapping("/manage")
    public String create(@Valid @ModelAttribute("form") NotificationManageForm form,
                         BindingResult result,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra,
                         Model model) {
        ensureCanManageNotifications(currentUser);
        if (!ALLOWED_TYPES.contains(form.getNotificationType())) {
            result.rejectValue("notificationType", "invalid", "通知種別が不正です");
        }
        if (!ALLOWED_SCOPES.contains(form.getTargetScope())) {
            result.rejectValue("targetScope", "invalid", "配信対象が不正です");
        }
        if (form.getScheduledAt() != null && form.getScheduledAt().isBefore(LocalDateTime.now())) {
            result.rejectValue("scheduledAt", "invalid", "予約日時は現在以降を指定してください");
        }
        if (result.hasErrors()) {
            int offset = 0;
            model.addAttribute("rows", notificationMapper.findCreatedByUserId(currentUser.id(), 20, offset));
            model.addAttribute("total", notificationMapper.countCreatedByUserId(currentUser.id()));
            model.addAttribute("page", 1);
            model.addAttribute("size", 20);
            return "notifications/manage";
        }

        boolean scheduled = form.getScheduledAt() != null;
        NotificationEntity row = new NotificationEntity(
                null,
                form.getNotificationType(),
                form.getTitle(),
                form.getBody(),
                form.getScheduledAt(),
                scheduled ? null : LocalDateTime.now(),
                scheduled ? "SCHEDULED" : "SENT",
                currentUser.id(),
                null,
                null,
                null
        );
        Long notificationId = notificationMapper.insertReturningId(row);
        if (notificationId == null) {
            log.error("Failed to insert notification record");
            ra.addFlashAttribute("error", "通知の作成に失敗しました。再度お試しください。");
            return "redirect:/app/notifications/manage";
        }
        List<Long> recipientIds = notificationMapper.findActiveRecipientUserIdsByScope(form.getTargetScope());
        for (Long uid : recipientIds) {
            notificationMapper.insertRecipient(notificationId, uid);
            notificationMapper.updateRecipientStatus(notificationId, uid, scheduled ? "PENDING" : "SENT");
        }
        if (!scheduled) {
            notificationMapper.updateStatus(notificationId, "SENT", LocalDateTime.now());
        }

        ra.addFlashAttribute("success", scheduled
                ? "通知を予約登録しました（" + recipientIds.size() + "件）"
                : "通知を配信しました（" + recipientIds.size() + "件）");
        return "redirect:/app/notifications/manage";
    }

    @PostMapping("/manage/{id}/send-now")
    public String sendNow(@PathVariable Long id,
                          @AuthenticationPrincipal LoginUser currentUser,
                          RedirectAttributes ra) {
        ensureCanManageNotifications(currentUser);
        NotificationEntity entity = notificationMapper.findById(id);
        if (entity == null) throw new BadRequestException("通知が見つかりません");
        if (!"SCHEDULED".equals(entity.deliveryStatus())) {
            throw new BadRequestException("予約済み通知のみ即時配信できます");
        }
        List<Long> recipients = notificationMapper.findRecipientUserIds(id);
        for (Long uid : recipients) {
            notificationMapper.updateRecipientStatus(id, uid, "SENT");
        }
        notificationMapper.updateStatus(id, "SENT", LocalDateTime.now());
        ra.addFlashAttribute("success", "予約通知を即時配信しました");
        return "redirect:/app/notifications/manage";
    }

    private void ensureCanManageNotifications(LoginUser currentUser) {
        if (currentUser == null || !currentUser.canManageOperations()) {
            throw new BadRequestException("通知配信管理は管理者・スタッフのみ利用できます");
        }
    }

    private List<ScheduleReminderRow> buildScheduleReminders(LoginUser currentUser, Set<String> dismissed) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime to = now.plusDays(30);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<ScheduleReminderRow> rows = new ArrayList<>();

        petService.listOwnedEntities(currentUser).forEach(pet -> {
            List<PetCareRecordEntity> upcoming = petCareRecordService.listUpcomingNotices(pet.id(), currentUser);
            upcoming.forEach(r -> {
                if (r.nextDueOn() == null) return;
                String key = "CARE:" + pet.id() + ":" + r.careType() + ":" + r.nextDueOn();
                LocalDateTime due = r.nextDueOn().atStartOfDay();
                rows.add(new ScheduleReminderRow(
                        key,
                        "REMINDER",
                        due,
                        "次回" + careTypeLabel(r.careType()) + "予定日",
                        pet.name() + " の " + careTypeLabel(r.careType()) + " 予定日: " + r.nextDueOn(),
                        null,
                        dismissed.contains(key)
                ));
            });
        });

        appointmentMapper.findRowsByOwnerUserId(currentUser.id(), 200, 0).stream()
                .filter(a -> a.scheduledAt() != null)
                .filter(a -> !a.scheduledAt().isBefore(now) && !a.scheduledAt().isAfter(to))
                .filter(a -> !"CANCELED".equals(a.status()))
                .forEach(a -> {
                    String key = "APPT:" + a.id();
                    boolean isZoom = "ONLINE".equals(a.channel());
                    rows.add(new ScheduleReminderRow(
                            key,
                            "REMINDER",
                            a.scheduledAt(),
                            isZoom ? "Zoom診療時間" : "診療予約日",
                            a.petName() + " の" + (isZoom ? " Zoom診療" : " 診療予約") + ": " + fmt.format(a.scheduledAt()),
                            isZoom && "CONFIRMED".equals(a.status()) ? a.zoomJoinUrl() : null,
                            dismissed.contains(key)
                    ));
                });

        subscriptionMapper.findUpcomingRenewalsByUserId(currentUser.id()).forEach(s -> {
            String key = "SUB:" + s.id() + ":" + s.endDate();
            String autoRenewNote = s.autoRenew() ? "（自動更新あり）" : "（自動更新なし）";
            rows.add(new ScheduleReminderRow(
                    key,
                    "INFO",
                    s.endDate().atStartOfDay(),
                    "サブスクリプション更新のお知らせ",
                    "お客様の " + s.planName() + " プランが " + s.endDate() + " に更新されます" + autoRenewNote,
                    null,
                    dismissed.contains(key)
            ));
        });

        return rows.stream()
                .sorted(Comparator.comparing(ScheduleReminderRow::scheduledAt))
                .limit(50)
                .toList();
    }

    private String careTypeLabel(String careType) {
        if ("RABIES".equals(careType)) return "狂犬病";
        if ("HEARTWORM".equals(careType)) return "フィラリア";
        if ("COMBO_VACCINE".equals(careType)) return "混合ワクチン";
        if ("MEDICAL_VISIT".equals(careType)) return "診療";
        return "ケア";
    }

    public record ScheduleReminderRow(
            String key,
            String notificationType,
            LocalDateTime scheduledAt,
            String title,
            String body,
            String actionUrl,
            boolean confirmed
    ) {}
}
