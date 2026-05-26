package com.example.petlife.service;

import com.example.petlife.dto.report.ReportStats;
import com.example.petlife.mapper.AppointmentMapper;
import com.example.petlife.mapper.HealthRecordMapper;
import com.example.petlife.mapper.PetMapper;
import com.example.petlife.mapper.SubscriptionMapper;
import com.example.petlife.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final UserMapper userMapper;
    private final PetMapper petMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final AppointmentMapper appointmentMapper;
    private final SubscriptionMapper subscriptionMapper;

    public ReportService(UserMapper userMapper,
                         PetMapper petMapper,
                         HealthRecordMapper healthRecordMapper,
                         AppointmentMapper appointmentMapper,
                         SubscriptionMapper subscriptionMapper) {
        this.userMapper = userMapper;
        this.petMapper = petMapper;
        this.healthRecordMapper = healthRecordMapper;
        this.appointmentMapper = appointmentMapper;
        this.subscriptionMapper = subscriptionMapper;
    }

    public ReportStats collect() {
        return new ReportStats(
                userMapper.countAll(),
                userMapper.countByRoleCode("ADMIN"),
                userMapper.countByRoleCode("SUPER"),
                userMapper.countByRoleCode("USER"),
                userMapper.countByRoleCode("VET"),
                userMapper.countByRoleCode("STAFF"),
                petMapper.countAll(),
                healthRecordMapper.countAll(),
                appointmentMapper.countAll(),
                appointmentMapper.countByStatus("REQUESTED"),
                appointmentMapper.countByStatus("CONFIRMED"),
                appointmentMapper.countByStatus("COMPLETED"),
                appointmentMapper.countByStatus("CANCELED"),
                subscriptionMapper.countAll()
        );
    }
}
