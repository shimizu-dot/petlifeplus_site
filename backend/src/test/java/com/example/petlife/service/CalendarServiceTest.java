package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.entity.PetEntity;
import com.example.petlife.entity.HealthRecordPetDateEntity;
import com.example.petlife.mapper.AppointmentMapper;
import com.example.petlife.mapper.AppointmentSlotMapper;
import com.example.petlife.mapper.CalendarMarkMapper;
import com.example.petlife.mapper.HealthRecordMapper;
import com.example.petlife.mapper.MedicalHistoryMapper;
import com.example.petlife.mapper.PetCareRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock PetService petService;
    @Mock CalendarMarkMapper calendarMarkMapper;
    @Mock HealthRecordMapper healthRecordMapper;
    @Mock AppointmentMapper appointmentMapper;
    @Mock AppointmentSlotMapper appointmentSlotMapper;
    @Mock PetCareRecordMapper petCareRecordMapper;
    @Mock MedicalHistoryMapper medicalHistoryMapper;

    @InjectMocks CalendarService calendarService;

    @Test
    void birthdayMarkShouldCarryPetNameForTooltip() {
        LoginUser owner = new LoginUser(3L, 3L, "申請者", "owner@petlife.local", "hash", true);
        PetEntity pet = new PetEntity(
                7L, owner.id(), "ポチ", "DOG", "Shiba", "MALE",
                LocalDate.of(2020, 6, 11), BigDecimal.valueOf(10.2), null,
                null, null, null, null);

        when(petService.listOwnedEntities(owner)).thenReturn(List.of(pet));
        when(calendarMarkMapper.findByOwnerUserIdAndDateRange(any(), any(), any())).thenReturn(List.of());
        when(healthRecordMapper.findRecordPetDatesByOwnerUserIdAndDateRange(any(), any(), any()))
                .thenReturn(List.of(new HealthRecordPetDateEntity(pet.id(), LocalDate.of(2026, 6, 11))));
        when(petCareRecordMapper.findVaccinePetDatesByOwnerAndDateRange(any(), any(), any()))
                .thenReturn(List.of(new HealthRecordPetDateEntity(pet.id(), LocalDate.of(2026, 6, 11))));
        when(medicalHistoryMapper.findMedicalHistoryPetDatesByOwnerAndDateRange(any(), any(), any()))
                .thenReturn(List.of(new HealthRecordPetDateEntity(pet.id(), LocalDate.of(2026, 6, 11))));

        CalendarService.CalendarView view = calendarService.buildMonthView(owner, YearMonth.of(2026, 6));

        CalendarService.DayMark birthday = view.days().stream()
                .filter(day -> day.date().equals(LocalDate.of(2026, 6, 11)))
                .flatMap(day -> day.marks().stream())
                .filter(mark -> "BIRTHDAY".equals(mark.type()))
                .findFirst()
                .orElseThrow();

        CalendarService.DayMark karte = view.days().stream()
                .filter(day -> day.date().equals(LocalDate.of(2026, 6, 11)))
                .flatMap(day -> day.marks().stream())
                .filter(mark -> "KARTE".equals(mark.type()))
                .findFirst()
                .orElseThrow();

        CalendarService.DayMark injection = view.days().stream()
                .filter(day -> day.date().equals(LocalDate.of(2026, 6, 11)))
                .flatMap(day -> day.marks().stream())
                .filter(mark -> "INJECTION".equals(mark.type()))
                .findFirst()
                .orElseThrow();

        CalendarService.DayMark doctor = view.days().stream()
                .filter(day -> day.date().equals(LocalDate.of(2026, 6, 11)))
                .flatMap(day -> day.marks().stream())
                .filter(mark -> "DOCTOR".equals(mark.type()))
                .findFirst()
                .orElseThrow();

        assertEquals("ポチ", birthday.petName());
        assertEquals("ポチ", karte.petName());
        assertEquals("ポチ", injection.petName());
        assertEquals("ポチ", doctor.petName());
    }
}
