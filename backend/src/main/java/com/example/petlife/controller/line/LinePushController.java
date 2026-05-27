package com.example.petlife.controller.line;

import com.example.petlife.service.line.LineBotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/app/admin/line/push")
public class LinePushController {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final LineBotService lineBotService;

    public LinePushController(LineBotService lineBotService) {
        this.lineBotService = lineBotService;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("lineConfigured", lineBotService.isConfigured());
        return "admin/line-push";
    }

    @PostMapping
    public String send(@RequestParam String message,
                       RedirectAttributes ra) {
        if (message == null || message.isBlank()) {
            ra.addFlashAttribute("error", "メッセージを入力してください");
            return "redirect:/app/admin/line/push";
        }
        if (!lineBotService.isConfigured()) {
            ra.addFlashAttribute("error", "LINE チャンネルトークンが設定されていません");
            return "redirect:/app/admin/line/push";
        }

        try {
            lineBotService.broadcastMessage(message);
            auditLog.info("action=line_broadcast_sent");
            ra.addFlashAttribute("success", "Bot の全フォロワーにメッセージを送信しました");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "送信に失敗しました: " + e.getMessage());
        }
        return "redirect:/app/admin/line/push";
    }
}
