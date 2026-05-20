package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.entity.PetCareRecordEntity;
import com.example.petlife.mapper.AppointmentMapper;
import com.example.petlife.mapper.DismissedReminderMapper;
import com.example.petlife.mapper.NotificationMapper;
import com.example.petlife.mapper.SubscriptionMapper;
import com.example.petlife.service.PetCareRecordService;
import com.example.petlife.service.PetService;
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
                    s.petName() + " の " + s.planName() + " プランが " + s.endDate() + " に更新されます" + autoRenewNote,
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
            boolean confirmed
    ) {}
}
