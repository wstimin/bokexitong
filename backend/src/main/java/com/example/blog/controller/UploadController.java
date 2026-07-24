package com.example.blog.controller;

import com.example.blog.common.Result;
import com.example.blog.security.BlogPrincipal;
import com.example.blog.service.RateLimitService;
import com.example.blog.service.UploadStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {
    private final RateLimitService rateLimitService;
    private final UploadStorageService uploadStorageService;

    public UploadController(RateLimitService rateLimitService, UploadStorageService uploadStorageService) {
        this.rateLimitService = rateLimitService;
        this.uploadStorageService = uploadStorageService;
    }

    @PostMapping
    public Result<Map<String, String>> upload(HttpServletRequest request,
                                              @AuthenticationPrincipal BlogPrincipal principal,
                                              @RequestParam("file") MultipartFile file) throws IOException {
        rateLimitService.check("upload:user:" + principal.userId(), 30, Duration.ofMinutes(10));
        rateLimitService.check("upload:ip:" + clientIp(request), 80, Duration.ofMinutes(10));
        UploadStorageService.StoredUpload stored = uploadStorageService.store(file);
        return Result.ok(Map.of(
                "url", stored.url(),
                "name", stored.originalName(),
                "contentType", stored.contentType()
        ));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
