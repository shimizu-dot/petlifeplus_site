package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.consultation.ConsultationForm;
import com.example.petlife.dto.consultation.MedicalHistoryRow;
import com.example.petlife.dto.common.PageResponse;
import com.example.petlife.entity.MedicalHistoryEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.mapper.MedicalHistoryMapper;
import com.example.petlife.mapper.PetMapper;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/app/consultations")
public class ConsultationController {

    private final MedicalHistoryMapper medicalHistoryMapper;
    private final PetMapper petMapper;

    public ConsultationController(MedicalHistoryMapper medicalHistoryMapper, PetMapper petMapper) {
        this.medicalHistoryMapper = medicalHistoryMapper;
        this.petMapper = petMapper;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model,
                       @AuthenticationPrincipal LoginUser currentUser) {
        if (!currentUser.canManageClinical()) {
            throw new BadRequestException("診療記録の閲覧には獣医師・スタッフのみアクセスできます");
        }
        int offset = (page - 1) * size;
        List<MedicalHistoryRow> items = medicalHistoryMapper.findAllRows(size, offset);
        long total = medicalHistoryMapper.countAll();
        model.addAttribute("page", new PageResponse<>(items, page, size, total));
        return "consultations/list";
    }

    @GetMapping("/new")
    public String newForm(Model model, @AuthenticationPrincipal LoginUser currentUser) {
        if (!currentUser.canManageClinical()) {
            throw new BadRequestException("診療記録の登録には獣医師・スタッフのみアクセスできます");
        }
        model.addAttribute("form", new ConsultationForm());
        model.addAttribute("pets", petMapper.findAll(200, 0));
        model.addAttribute("editMode", false);
        return "consultations/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ConsultationForm form,
                         BindingResult result,
                         Model model,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        if (!currentUser.canManageClinical()) {
            throw new BadRequestException("診療記録の登録には獣医師・スタッフのみアクセスできます");
        }
        if (result.hasErrors()) {
            model.addAttribute("pets", petMapper.findAll(200, 0));
            model.addAttribute("editMode", false);
            return "consultations/form";
        }
        MedicalHistoryEntity entity = new MedicalHistoryEntity(
                null, form.getPetId(), form.getAppointmentId(), currentUser.id(),
                form.getPerformedOn(), form.getTreatmentDetail(),
                form.getDiagnosis(), form.getPrescription(),
                null, null, null);
        medicalHistoryMapper.insertReturningId(entity);
        ra.addFlashAttribute("success", "診療記録を登録しました");
        return "redirect:/app/consultations";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           Model model,
                           @AuthenticationPrincipal LoginUser currentUser) {
        if (!currentUser.canManageClinical()) {
            throw new BadRequestException("診療記録の編集には獣医師・スタッフのみアクセスできます");
        }
        MedicalHistoryEntity entity = medicalHistoryMapper.findById(id);
        if (entity == null) throw new BadRequestException("記録が見つかりません");

        ConsultationForm form = new ConsultationForm();
        form.setPetId(entity.petId());
        form.setAppointmentId(entity.appointmentId());
        form.setPerformedOn(entity.performedOn());
        form.setTreatmentDetail(entity.treatmentDetail());
        form.setDiagnosis(entity.diagnosis());
        form.setPrescription(entity.prescription());

        model.addAttribute("form", form);
        model.addAttribute("consultationId", id);
        model.addAttribute("pets", petMapper.findAll(200, 0));
        model.addAttribute("editMode", true);
        return "consultations/form";
    }

    @PatchMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") ConsultationForm form,
                         BindingResult result,
                         Model model,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        if (!currentUser.canManageClinical()) {
            throw new BadRequestException("診療記録の編集には獣医師・スタッフのみアクセスできます");
        }
        MedicalHistoryEntity existing = medicalHistoryMapper.findById(id);
        if (existing == null) throw new BadRequestException("記録が見つかりません");

        if (result.hasErrors()) {
            model.addAttribute("consultationId", id);
            model.addAttribute("pets", petMapper.findAll(200, 0));
            model.addAttribute("editMode", true);
            return "consultations/form";
        }
        MedicalHistoryEntity updated = new MedicalHistoryEntity(
                id, form.getPetId(), form.getAppointmentId(), existing.handledByUserId(),
                form.getPerformedOn(), form.getTreatmentDetail(),
                form.getDiagnosis(), form.getPrescription(),
                null, null, null);
        medicalHistoryMapper.update(updated);
        ra.addFlashAttribute("success", "診療記録を更新しました");
        return "redirect:/app/consultations";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        if (!currentUser.canManageClinical()) {
            throw new BadRequestException("診療記録の削除には獣医師・スタッフのみアクセスできます");
        }
        medicalHistoryMapper.softDelete(id, LocalDateTime.now());
        ra.addFlashAttribute("success", "診療記録を削除しました");
        return "redirect:/app/consultations";
    }
}
