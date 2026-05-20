package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.premium.PremiumOnlineCareForm;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.service.AppointmentService;
import com.example.petlife.service.PetService;
import com.example.petlife.service.PlanAccessService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/app/premium/online-care")
public class PremiumSupportController {

    private final PlanAccessService planAccessService;
    private final PetService petService;
    private final AppointmentService appointmentService;

    public PremiumSupportController(PlanAccessService planAccessService,
                                    PetService petService,
                                    AppointmentService appointmentService) {
        this.planAccessService = planAccessService;
        this.petService = petService;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public String form(Model model, @AuthenticationPrincipal LoginUser currentUser) {
        if (!planAccessService.canUsePrioritySupport(currentUser)) {
            throw new BadRequestException("この機能はプレミアムプランで利用できます");
        }
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new PremiumOnlineCareForm());
        }
        model.addAttribute("pets", petService.list(1, 100, currentUser).items().stream()
                .filter(p -> p.deceasedAt() == null)
                .toList());
        return "premium/online-care";
    }

    @PostMapping
    public String create(@Valid PremiumOnlineCareForm form,
                         BindingResult result,
                         Model model,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        if (!planAccessService.canUsePrioritySupport(currentUser)) {
            throw new BadRequestException("この機能はプレミアムプランで利用できます");
        }
        if (result.hasErrors()) {
            model.addAttribute("pets", petService.list(1, 100, currentUser).items().stream()
                    .filter(p -> p.deceasedAt() == null)
                    .toList());
            return "premium/online-care";
        }

        AppointmentService.PremiumOnlineCareResult resultObj = appointmentService.createPremiumOnlineCare(
                form.getPetId(),
                form.getScheduledAt(),
                form.getNote(),
                currentUser
        );

        ra.addFlashAttribute("success", "オンライン診療を予約しました");
        ra.addFlashAttribute("zoomJoinUrl", resultObj.appointment().zoomJoinUrl());
        if (resultObj.zoomFallbackUsed()) {
            ra.addFlashAttribute("zoomFallbackWarning",
                    "Zoom API連携に失敗したため代替リンクを発行しました。理由: " + resultObj.zoomFallbackReason());
        }
        return "redirect:/app/premium/online-care";
    }
}
