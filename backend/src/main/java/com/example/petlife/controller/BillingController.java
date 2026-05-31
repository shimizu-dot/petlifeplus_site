package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.billing.InvoiceRow;
import com.example.petlife.dto.billing.PaymentForm;
import com.example.petlife.dto.common.PageResponse;
import com.example.petlife.entity.PaymentEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/app/admin/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    /** 請求一覧 */
    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model,
                       @AuthenticationPrincipal LoginUser currentUser) {
        model.addAttribute("bodyClass", "role-admin");
        List<InvoiceRow> items = billingService.findAllWithDetails(page, size);
        long total = billingService.countAll();
        model.addAttribute("page", new PageResponse<>(items, page, size, total));
        return "admin/billing/index";
    }

    /** 請求詳細 + 入金登録フォーム */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         Model model,
                         @AuthenticationPrincipal LoginUser currentUser) {
        model.addAttribute("bodyClass", "role-admin");
        InvoiceRow invoice = billingService.findByIdWithDetails(id);
        List<PaymentEntity> payments = billingService.findPaymentsByInvoiceId(id);
        model.addAttribute("invoice", invoice);
        model.addAttribute("payments", payments);
        model.addAttribute("form", new PaymentForm());
        return "admin/billing/detail";
    }

    /** 通知を再送（既存請求書に対してアプリ内通知 + メール + LINE を再送信） */
    @PostMapping("/{id}/resend-notification")
    public String resendNotification(@PathVariable Long id,
                                     @AuthenticationPrincipal LoginUser currentUser,
                                     RedirectAttributes ra) {
        billingService.resendNotification(id);
        ra.addFlashAttribute("success", "お支払いのご案内を再送しました");
        return "redirect:/app/admin/billing/" + id;
    }

    /** 入金登録 */
    @PostMapping("/{id}/payments")
    public String registerPayment(@PathVariable Long id,
                                  @Valid @ModelAttribute("form") PaymentForm form,
                                  BindingResult result,
                                  @AuthenticationPrincipal LoginUser currentUser,
                                  Model model,
                                  RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("bodyClass", "role-admin");
            model.addAttribute("invoice", billingService.findByIdWithDetails(id));
            model.addAttribute("payments", billingService.findPaymentsByInvoiceId(id));
            model.addAttribute("form", form);
            return "admin/billing/detail";
        }
        try {
            billingService.registerPayment(id, form);
        } catch (BadRequestException e) {
            model.addAttribute("bodyClass", "role-admin");
            model.addAttribute("invoice", billingService.findByIdWithDetails(id));
            model.addAttribute("payments", billingService.findPaymentsByInvoiceId(id));
            model.addAttribute("form", form);
            model.addAttribute("error", e.getMessage());
            return "admin/billing/detail";
        }
        ra.addFlashAttribute("success", "入金を登録しました");
        return "redirect:/app/admin/billing/" + id;
    }
}
