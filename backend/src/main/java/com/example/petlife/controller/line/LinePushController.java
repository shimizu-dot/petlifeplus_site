package com.example.petlife.controller.line;

import com.example.petlife.entity.UserEntity;
import com.example.petlife.mapper.UserMapper;
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

import java.util.List;

@Controller
@RequestMapping("/app/admin/line/push")
public class LinePushController {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final LineBotService lineBotService;
    private final UserMapper userMapper;

    public LinePushController(LineBotService lineBotService, UserMapper userMapper) {
        this.lineBotService = lineBotService;
        this.userMapper = userMapper;
    }

    @GetMapping
    public String page(Model model) {
        List<UserEntity> users = userMapper.findAllWithLineId();
        model.addAttribute("lineUserCount", users.size());
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

        List<UserEntity> users = userMapper.findAllWithLineId();
        List<String> lineIds = users.stream()
                .map(UserEntity::lineUserId)
                .toList();

        int sent = lineBotService.multicastMessage(lineIds, message);
        auditLog.info("action=line_push_sent count={}", sent);
        ra.addFlashAttribute("success", sent + " 名の LINE 登録ユーザーにメッセージを送信しました");
        return "redirect:/app/admin/line/push";
    }
}
