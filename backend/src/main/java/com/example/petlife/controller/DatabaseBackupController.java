package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.exception.ForbiddenException;
import com.example.petlife.service.DatabaseBackupService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/app/admin/database")
public class DatabaseBackupController {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupController.class);

    private final DatabaseBackupService backupService;

    public DatabaseBackupController(DatabaseBackupService backupService) {
        this.backupService = backupService;
    }

    @GetMapping
    public String page(@AuthenticationPrincipal LoginUser currentUser) {
        if (!currentUser.isAdmin()) throw new ForbiddenException("管理者のみ利用できます");
        return "admin/database";
    }

    @PostMapping("/backup")
    public String backup(@AuthenticationPrincipal LoginUser currentUser,
                         HttpServletResponse response, RedirectAttributes ra) {
        if (!currentUser.isAdmin()) throw new ForbiddenException("管理者のみ利用できます");
        try {
            byte[] data = backupService.backup();
            String filename = "petlife_backup_" + LocalDate.now() + ".sql";
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentLength(data.length);
            response.getOutputStream().write(data);
            response.flushBuffer();
            return null;
        } catch (Exception e) {
            log.error("Database backup failed", e);
            ra.addFlashAttribute("error", "バックアップに失敗しました: " + e.getMessage());
            return "redirect:/app/admin/database";
        }
    }

    @PostMapping("/restore")
    public String restore(@AuthenticationPrincipal LoginUser currentUser,
                          @RequestParam MultipartFile file, RedirectAttributes ra) {
        if (!currentUser.isAdmin()) throw new ForbiddenException("管理者のみ利用できます");
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "ファイルを選択してください");
            return "redirect:/app/admin/database";
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".sql")) {
            ra.addFlashAttribute("error", "SQLファイル（.sql）のみアップロードできます");
            return "redirect:/app/admin/database";
        }
        try {
            byte[] content = file.getBytes();
            backupService.restore(content);
            log.info("Database restored from file: {}, size: {} bytes", originalName, content.length);
            ra.addFlashAttribute("success", "データベースのリストアが完了しました");
        } catch (Exception e) {
            log.error("Database restore failed", e);
            ra.addFlashAttribute("error", "リストアに失敗しました: " + e.getMessage());
        }
        return "redirect:/app/admin/database";
    }
}
