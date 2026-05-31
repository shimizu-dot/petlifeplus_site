package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.billing.InvoiceRow;
import com.example.petlife.exception.ForbiddenException;
import com.example.petlife.exception.NotFoundException;
import com.example.petlife.mapper.InvoiceMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/app/invoices")
public class UserInvoiceController {

    private final InvoiceMapper invoiceMapper;

    public UserInvoiceController(InvoiceMapper invoiceMapper) {
        this.invoiceMapper = invoiceMapper;
    }

    /** 請求書・領収書ビュー（請求書オーナーまたは ADMIN/SUPER のみアクセス可） */
    @GetMapping("/{id}")
    public String view(@PathVariable Long id,
                       Model model,
                       @AuthenticationPrincipal LoginUser currentUser) {
        InvoiceRow invoice = invoiceMapper.findByIdWithDetails(id);
        if (invoice == null) throw new NotFoundException("請求書が見つかりません: " + id);

        if (!currentUser.isAdmin() && !invoice.ownerUserId().equals(currentUser.id())) {
            throw new ForbiddenException("この請求書にアクセスする権限がありません");
        }

        model.addAttribute("invoice", invoice);
        model.addAttribute("isPaid", "PAID".equals(invoice.paymentStatus()));
        return "invoices/view";
    }
}
