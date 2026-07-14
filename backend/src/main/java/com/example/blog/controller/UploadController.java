package com.example.blog.controller;

import com.example.blog.common.Result;
import com.example.blog.security.BlogPrincipal;
import com.example.blog.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/uploads")
public class UploadController {
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp",
            ".mp4", ".webm",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".md", ".zip"
    );
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/webm",
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain", "text/markdown", "application/zip", "application/x-zip-compressed"
    );

    private final Path uploadRoot;
    private final RateLimitService rateLimitService;

    public UploadController(@Value("${blog.upload-dir}") String uploadDir, RateLimitService rateLimitService) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    public Result<Map<String, String>> upload(HttpServletRequest request,
                                              @AuthenticationPrincipal BlogPrincipal principal,
                                              @RequestParam("file") MultipartFile file) throws IOException {
        rateLimitService.check("upload:user:" + principal.userId(), 30, Duration.ofMinutes(10));
        rateLimitService.check("upload:ip:" + clientIp(request), 80, Duration.ofMinutes(10));
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("单个文件不能超过 50MB");
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = getExtension(original);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension) || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("不支持的文件类型");
        }
        validateFileSignature(file, extension, contentType);

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
        return Result.ok(Map.of("url", url, "name", original, "contentType", contentType));
    }

    private void validateFileSignature(MultipartFile file, String extension, String contentType) throws IOException {
        byte[] header = readHeader(file, 16);
        if (contentType.startsWith("image/")) {
            boolean validImage = (Set.of(".jpg", ".jpeg").contains(extension) && startsWith(header, 0xFF, 0xD8, 0xFF))
                    || (".png".equals(extension) && startsWith(header, 0x89, 0x50, 0x4E, 0x47))
                    || (".gif".equals(extension) && startsWithAscii(header, "GIF8"))
                    || (".webp".equals(extension) && startsWithAscii(header, "RIFF") && asciiAt(header, 8, "WEBP"));
            if (!validImage) throw new IllegalArgumentException("图片文件内容不合法");
        }
        if ("application/pdf".equals(contentType) && !startsWithAscii(header, "%PDF")) {
            throw new IllegalArgumentException("PDF 文件内容不合法");
        }
        if ((".docx".equals(extension) || ".xlsx".equals(extension) || ".pptx".equals(extension) || ".zip".equals(extension))
                && !startsWith(header, 0x50, 0x4B)) {
            throw new IllegalArgumentException("压缩类文件内容不合法");
        }
        if ((".mp4".equals(extension) || ".webm".equals(extension)) && file.getSize() < 16) {
            throw new IllegalArgumentException("视频文件内容不合法");
        }
    }

    private byte[] readHeader(MultipartFile file, int size) throws IOException {
        byte[] header = new byte[size];
        try (InputStream input = file.getInputStream()) {
            int read = input.read(header);
            if (read <= 0) return new byte[0];
        }
        return header;
    }

    private boolean startsWith(byte[] bytes, int... values) {
        if (bytes.length < values.length) return false;
        for (int i = 0; i < values.length; i++) {
            if ((bytes[i] & 0xFF) != values[i]) return false;
        }
        return true;
    }

    private boolean startsWithAscii(byte[] bytes, String value) {
        return asciiAt(bytes, 0, value);
    }

    private boolean asciiAt(byte[] bytes, int offset, String value) {
        if (bytes.length < offset + value.length()) return false;
        for (int i = 0; i < value.length(); i++) {
            if (bytes[offset + i] != (byte) value.charAt(i)) return false;
        }
        return true;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            throw new IllegalArgumentException("文件缺少扩展名");
        }
        return filename.substring(dot).toLowerCase();
    }
}
