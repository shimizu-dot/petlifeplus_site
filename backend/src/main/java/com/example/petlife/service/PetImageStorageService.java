package com.example.petlife.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class PetImageStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("画像ファイル（jpeg/png/webp/gif）のみアップロードできます");
        }

        String filename = UUID.randomUUID() + ".png";
        Path dir = Paths.get("uploads", "pets");
        Path dst = dir.resolve(filename);
        try {
            Files.createDirectories(dir);
            BufferedImage src = ImageIO.read(file.getInputStream());
            if (src == null) {
                throw new IllegalArgumentException("画像の読み込みに失敗しました。jpeg/png/gif 形式を利用してください");
            }
            int size = 100;
            int sw = src.getWidth(), sh = src.getHeight();
            int cropX = 0, cropY = 0, cropSize = Math.min(sw, sh);
            if (sw > sh) cropX = (sw - cropSize) / 2;
            else         cropY = (sh - cropSize) / 2;
            BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(src, 0, 0, size, size, cropX, cropY, cropX + cropSize, cropY + cropSize, null);
            g.dispose();
            ImageIO.write(resized, "png", dst.toFile());
        } catch (IOException e) {
            throw new RuntimeException("画像保存に失敗しました", e);
        }
        return "/uploads/pets/" + filename;
    }
}
