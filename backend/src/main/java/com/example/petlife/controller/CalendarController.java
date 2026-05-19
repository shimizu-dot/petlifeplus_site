package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.calendar.CalendarMarkForm;
import com.example.petlife.service.CalendarService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;

@Controller
@RequestMapping("/app/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping
    public String page(@RequestParam(required = false) String month,
                       Model model,
                       @AuthenticationPrincipal LoginUser currentUser) {
        YearMonth ym = parseMonthOrNow(month);
        model.addAttribute("calendar", calendarService.buildMonthView(currentUser, ym));
        model.addAttribute("markForm", new CalendarMarkForm());
        return "calendar/index";
    }

    @PostMapping("/marks/add")
    public String addMark(@Valid @ModelAttribute("markForm") CalendarMarkForm form,
                          @RequestParam String month,
                          @AuthenticationPrincipal LoginUser currentUser,
                          RedirectAttributes ra) {
        calendarService.addMark(currentUser, form);
        ra.addFlashAttribute("success", "シールを追加しました");
        return "redirect:/app/calendar?month=" + month;
    }

    @PostMapping("/marks/{id}/delete")
    public String deleteMark(@PathVariable Long id,
                             @RequestParam String month,
                             @AuthenticationPrincipal LoginUser currentUser,
                             RedirectAttributes ra) {
        calendarService.deleteMark(currentUser, id);
        ra.addFlashAttribute("success", "シールを外しました");
        return "redirect:/app/calendar?month=" + month;
    }

    private YearMonth parseMonthOrNow(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (Exception e) {
            return YearMonth.now();
        }
    }
}
