package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.entity.PetEntity;
import com.example.petlife.mapper.HealthRecordMapper;
import com.example.petlife.mapper.PetMapper;
import com.example.petlife.mapper.UserMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/app")
public class DashboardController {

    private final PetMapper petMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final UserMapper userMapper;

    public DashboardController(PetMapper petMapper, HealthRecordMapper healthRecordMapper, UserMapper userMapper) {
        this.petMapper = petMapper;
        this.healthRecordMapper = healthRecordMapper;
        this.userMapper = userMapper;
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
        return "dashboard/index";
    }
}
