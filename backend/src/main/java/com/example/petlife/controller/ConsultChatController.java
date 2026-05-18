package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.chat.ConsultChatForm;
import com.example.petlife.service.ConsultChatService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/app/consult/chatbot")
public class ConsultChatController {

    private final ConsultChatService consultChatService;

    public ConsultChatController(ConsultChatService consultChatService) {
        this.consultChatService = consultChatService;
    }

    @GetMapping
    public String page(Model model, @AuthenticationPrincipal LoginUser currentUser) {
        model.addAttribute("form", new ConsultChatForm());
        model.addAttribute("messages", consultChatService.getRecentMessages(currentUser));
        return "consult/chatbot";
    }

    @PostMapping
    public String send(@Valid ConsultChatForm form,
                       BindingResult result,
                       @AuthenticationPrincipal LoginUser currentUser,
                       RedirectAttributes ra,
                       Model model) {
        if (result.hasErrors()) {
            model.addAttribute("messages", consultChatService.getRecentMessages(currentUser));
            return "consult/chatbot";
        }
        consultChatService.postUserMessage(currentUser, form.getMessage());
        ra.addFlashAttribute("success", "メッセージを送信しました");
        return "redirect:/app/consult/chatbot";
    }
}
