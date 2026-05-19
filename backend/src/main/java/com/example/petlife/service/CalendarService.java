package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.calendar.CalendarMarkForm;
import com.example.petlife.entity.CalendarMarkEntity;
import com.example.petlife.entity.HealthRecordPetDateEntity;
import com.example.petlife.entity.PetEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.mapper.CalendarMarkMapper;
import com.example.petlife.mapper.HealthRecordMapper;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CalendarService {

    private static final Set<String> ALLOWED_MARK_TYPES = Set.of("KARTE", "VACCINE", "HOSPITAL", "TRIMMING", "DOG_RUN", "MEDICINE");
    private static final String[] PET_COLOR_CLASSES = {"pet-c1", "pet-c2", "pet-c3", "pet-c4", "pet-c5", "pet-c6"};

    private final PetService petService;
    private final CalendarMarkMapper calendarMarkMapper;
    private final HealthRecordMapper healthRecordMapper;

    public CalendarService(PetService petService,
                           CalendarMarkMapper calendarMarkMapper,
                           HealthRecordMapper healthRecordMapper) {
        this.petService = petService;
        this.calendarMarkMapper = calendarMarkMapper;
        this.healthRecordMapper = healthRecordMapper;
    }

    public CalendarView buildMonthView(LoginUser user, YearMonth ym) {
        LocalDate first = ym.atDay(1);
        LocalDate start = first.minusDays(first.getDayOfWeek().getValue() % 7L);
        LocalDate end = ym.atEndOfMonth();
        int remain = 6 - (end.getDayOfWeek().getValue() % 7);
        LocalDate finish = end.plusDays(remain);

        List<PetEntity> pets = petService.listOwnedEntities(user);
        Map<Long, String> petColors = new HashMap<>();
        for (PetEntity pet : pets) {
            petColors.put(pet.id(), colorClassForPet(pet.id()));
        }

        Map<LocalDate, List<DayMark>> marksByDate = new HashMap<>();

        List<CalendarMarkEntity> manual = calendarMarkMapper.findByOwnerUserIdAndDateRange(user.id(), start, finish);
        for (CalendarMarkEntity mark : manual) {
            addMark(marksByDate, mark.markDate(), new DayMark(
                    mark.id(),
                    toSticker(mark.markType()),
                    false,
                    mark.petId(),
                    petColors.getOrDefault(mark.petId(), "")
            ));
        }

        List<HealthRecordPetDateEntity> recordDates = healthRecordMapper.findRecordPetDatesByOwnerUserIdAndDateRange(user.id(), start, finish);
        for (HealthRecordPetDateEntity d : recordDates) {
            addMark(marksByDate, d.recordDate(), new DayMark(
                    null,
                    "KARTE",
                    true,
                    d.petId(),
                    petColors.getOrDefault(d.petId(), "")
            ));
        }

        for (PetEntity pet : pets) {
            if (pet.birthDate() == null) {
                continue;
            }
            for (LocalDate d = start; !d.isAfter(finish); d = d.plusDays(1)) {
                if (d.getMonthValue() == pet.birthDate().getMonthValue()
                        && d.getDayOfMonth() == pet.birthDate().getDayOfMonth()) {
                    addMark(marksByDate, d, new DayMark(
                            null,
                            "BIRTHDAY",
                            true,
                            pet.id(),
                            petColors.getOrDefault(pet.id(), "")
                    ));
                }
            }
        }

        List<DayCell> days = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(finish); d = d.plusDays(1)) {
            days.add(new DayCell(
                    d,
                    d.getMonthValue() == ym.getMonthValue(),
                    d.getDayOfWeek() == DayOfWeek.SUNDAY,
                    isJapaneseHoliday(d),
                    new ArrayList<>(marksByDate.getOrDefault(d, List.of()))));
        }

        List<PetBadge> petBadges = new ArrayList<>();
        for (PetEntity pet : pets) {
            petBadges.add(new PetBadge(pet.id(), pet.name(), petColors.getOrDefault(pet.id(), "")));
        }

        return new CalendarView(ym, days, petBadges);
    }

    public void addMark(LoginUser user, CalendarMarkForm form) {
        if (!ALLOWED_MARK_TYPES.contains(form.getMarkType())) {
            throw new BadRequestException("不正なシール種別です");
        }
        PetEntity pet = petService.getEntity(form.getPetId(), user);
        CalendarMarkEntity row = new CalendarMarkEntity(
                null,
                pet.id(),
                user.id(),
                form.getMarkDate(),
                form.getMarkType(),
                null,
                null,
                null,
                null
        );
        calendarMarkMapper.insertReturningId(row);
    }

    public void deleteMark(LoginUser user, Long markId) {
        CalendarMarkEntity mark = calendarMarkMapper.findOwnedActiveById(markId, user.id());
        if (mark == null) {
            throw new BadRequestException("指定のシールが見つかりません");
        }
        calendarMarkMapper.softDeleteOwnedMark(markId, user.id());
    }

    private void addMark(Map<LocalDate, List<DayMark>> marksByDate, LocalDate date, DayMark dayMark) {
        marksByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(dayMark);
    }

    private String colorClassForPet(Long petId) {
        int idx = (int) (Math.abs(petId) % PET_COLOR_CLASSES.length);
        return PET_COLOR_CLASSES[idx];
    }

    private String toSticker(String markType) {
        return switch (markType) {
            case "KARTE" -> "KARTE";
            case "VACCINE" -> "INJECTION";
            case "HOSPITAL" -> "DOCTOR";
            case "TRIMMING" -> "SCISSORS";
            case "DOG_RUN" -> "PARK";
            case "MEDICINE" -> "CAPSULE";
            default -> "";
        };
    }

    private boolean isJapaneseHoliday(LocalDate date) {
        if (isFixedHoliday(date) || isHappyMondayHoliday(date) || isEquinoxHoliday(date)) {
            return true;
        }
        LocalDate prev = date.minusDays(1);
        if (isPrimaryHoliday(prev) && date.getDayOfWeek() == DayOfWeek.MONDAY) {
            return true;
        }
        LocalDate next = date.plusDays(1);
        return isPrimaryHoliday(prev) && isPrimaryHoliday(next);
    }

    private boolean isPrimaryHoliday(LocalDate date) {
        return isFixedHoliday(date) || isHappyMondayHoliday(date) || isEquinoxHoliday(date);
    }

    private boolean isFixedHoliday(LocalDate date) {
        int m = date.getMonthValue();
        int d = date.getDayOfMonth();
        return (m == 1 && d == 1)
                || (m == 2 && d == 11)
                || (m == 2 && d == 23)
                || (m == 4 && d == 29)
                || (m == 5 && d == 3)
                || (m == 5 && d == 4)
                || (m == 5 && d == 5)
                || (m == 8 && d == 11)
                || (m == 11 && d == 3)
                || (m == 11 && d == 23);
    }

    private boolean isHappyMondayHoliday(LocalDate date) {
        return isNthWeekday(date, Month.JANUARY, DayOfWeek.MONDAY, 2)
                || isNthWeekday(date, Month.JULY, DayOfWeek.MONDAY, 3)
                || isNthWeekday(date, Month.SEPTEMBER, DayOfWeek.MONDAY, 3)
                || isNthWeekday(date, Month.OCTOBER, DayOfWeek.MONDAY, 2);
    }

    private boolean isNthWeekday(LocalDate date, Month month, DayOfWeek dayOfWeek, int nth) {
        if (date.getMonth() != month || date.getDayOfWeek() != dayOfWeek) {
            return false;
        }
        int occurrence = (date.getDayOfMonth() - 1) / 7 + 1;
        return occurrence == nth;
    }

    private boolean isEquinoxHoliday(LocalDate date) {
        int year = date.getYear();
        int day;
        if (date.getMonth() == Month.MARCH) {
            day = (int) Math.floor(20.8431 + 0.242194 * (year - 1980) - Math.floor((year - 1980) / 4.0));
            return date.getDayOfMonth() == day;
        }
        if (date.getMonth() == Month.SEPTEMBER) {
            day = (int) Math.floor(23.2488 + 0.242194 * (year - 1980) - Math.floor((year - 1980) / 4.0));
            return date.getDayOfMonth() == day;
        }
        return false;
    }

    public record DayMark(Long id, String type, boolean auto, Long petId, String colorClass) {}

    public record DayCell(LocalDate date, boolean inCurrentMonth, boolean sunday, boolean holiday, List<DayMark> marks) {}

    public record PetBadge(Long id, String name, String colorClass) {}

    public record CalendarView(YearMonth month, List<DayCell> days, List<PetBadge> pets) {
        public String monthLabel() {
            return month.getYear() + "年" + month.getMonthValue() + "月";
        }

        public String prevMonth() {
            YearMonth prev = month.minusMonths(1);
            return prev.getYear() + "-" + String.format("%02d", prev.getMonthValue());
        }

        public String nextMonth() {
            YearMonth next = month.plusMonths(1);
            return next.getYear() + "-" + String.format("%02d", next.getMonthValue());
        }
    }
}
