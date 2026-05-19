package com.example.petlife.controller;

import com.example.petlife.mapper.AppointmentMapper;
import com.example.petlife.mapper.HealthRecordMapper;
import com.example.petlife.mapper.PetMapper;
import com.example.petlife.mapper.SubscriptionMapper;
import com.example.petlife.mapper.UserMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/app/reports")
public class ReportController {

    private final UserMapper userMapper;
    private final PetMapper petMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final AppointmentMapper appointmentMapper;
    private final SubscriptionMapper subscriptionMapper;

    public ReportController(UserMapper userMapper, PetMapper petMapper,
                            HealthRecordMapper healthRecordMapper,
                            AppointmentMapper appointmentMapper,
                            SubscriptionMapper subscriptionMapper) {
        this.userMapper = userMapper;
        this.petMapper = petMapper;
        this.healthRecordMapper = healthRecordMapper;
        this.appointmentMapper = appointmentMapper;
        this.subscriptionMapper = subscriptionMapper;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("userCount", userMapper.countAll());
        model.addAttribute("userCountAdmin", userMapper.countByRoleId(1L));
        model.addAttribute("userCountUser", userMapper.countByRoleId(2L));
        model.addAttribute("userCountVet", userMapper.countByRoleId(3L));
        model.addAttribute("userCountStaff", userMapper.countByRoleId(4L));

        model.addAttribute("petCount", petMapper.countAll());
        model.addAttribute("healthRecordCount", healthRecordMapper.countAll());

        model.addAttribute("appointmentCount", appointmentMapper.countAll());
        model.addAttribute("appointmentRequested", appointmentMapper.countByStatus("REQUESTED"));
        model.addAttribute("appointmentConfirmed", appointmentMapper.countByStatus("CONFIRMED"));
        model.addAttribute("appointmentCompleted", appointmentMapper.countByStatus("COMPLETED"));
        model.addAttribute("appointmentCanceled", appointmentMapper.countByStatus("CANCELED"));

        model.addAttribute("subscriptionCount", subscriptionMapper.countAll());
        return "reports/index";
    }
}
