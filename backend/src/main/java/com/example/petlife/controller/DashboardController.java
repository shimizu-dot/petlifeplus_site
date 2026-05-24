package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.entity.PetEntity;
import com.example.petlife.service.AnnouncementService;
import com.example.petlife.mapper.HealthRecordMapper;
import java.util.List;
import com.example.petlife.mapper.PetMapper;
import com.example.petlife.mapper.UserMapper;
import com.example.petlife.service.PlanAccessService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/app")
public class DashboardController {

    private final PetMapper petMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final UserMapper userMapper;
    private final PlanAccessService planAccessService;
    private final AnnouncementService announcementService;

    public DashboardController(PetMapper petMapper, HealthRecordMapper healthRecordMapper, UserMapper userMapper,
                               PlanAccessService planAccessService, AnnouncementService announcementService) {
        this.petMapper = petMapper;
        this.healthRecordMapper = healthRecordMapper;
        this.userMapper = userMapper;
        this.planAccessService = planAccessService;
        this.announcementService = announcementService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal LoginUser currentUser) {
        if (currentUser.isAdmin()) {
            model.addAttribute("petCount",    petMapper.countAll());
            model.addAttribute("recordCount", healthRecordMapper.countAll());
            model.addAttribute("userCount",   userMapper.countAll());
        } else {
            long myPets = petMapper.countByOwnerUserId(currentUser.id());
            long myRecords = 0L;
            if (myPets > 0) {
                List<PetEntity> pets = petMapper.findByOwnerUserId(currentUser.id(), 100, 0);
                myRecords = pets.stream()
                        .mapToLong(p -> healthRecordMapper.countByPetId(p.id()))
                        .sum();
            }
            model.addAttribute("petCount",    myPets);
            model.addAttribute("recordCount", myRecords);
            model.addAttribute("userCount",   0L);
        }
        model.addAttribute("planLabel", planAccessService.planLabel(currentUser));
        model.addAttribute("canUseAiSymptom", planAccessService.canUseAiSymptom(currentUser));
        model.addAttribute("canUsePrioritySupport", planAccessService.canUsePrioritySupport(currentUser));
        model.addAttribute("announcements", announcementService.findActive());
        return "dashboard/index";
    }
}
