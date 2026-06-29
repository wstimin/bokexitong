package com.example.blog.controller;

import com.example.blog.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/uploads")
public class UploadController {
    private final Path uploadRoot;

    public UploadController(@Value("${blog.upload-dir}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @PostMapping
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot).toLowerCase();
        }
        LocalDate today = LocalDate.now();
        Path dir = uploadRoot.resolve(String.valueOf(today.getYear()))
                .resolve(String.format("%02d", today.getMonthValue()))
                .resolve(String.format("%02d", today.getDayOfMonth()))
                .normalize();
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + extension;
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("文件名不合法");
        }
        file.transferTo(target);
        String url = "/api/uploads/" + uploadRoot.relativize(target).toString().replace('\\', '/');
        return Result.ok(Map.of("url", url, "name", original, "contentType", file.getContentType() == null ? "" : file.getContentType()));
    }
}
