package com.example.blog.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

@Controller
public class PageController {
    private final ResourceLoader resourceLoader;
    private final String webRoot;

    public PageController(ResourceLoader resourceLoader,
                          @Value("${blog.web-root:file:./frontend/dist/}") String webRoot) {
        this.resourceLoader = resourceLoader;
        this.webRoot = normalizeLocation(webRoot);
    }

    @GetMapping("/")
    @ResponseBody
    public ResponseEntity<Resource> page() throws IOException {
        Resource index = resourceLoader.getResource(webRoot + "index.html");
        if (!index.exists() || !index.isReadable()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.TEXT_HTML)
                .body(index);
    }

    private String normalizeLocation(String location) {
        String trimmed = location == null ? "" : location.trim();
        if (trimmed.isEmpty()) {
            return "file:./frontend/dist/";
        }
        if (!trimmed.endsWith("/")) {
            trimmed += "/";
        }
        if (trimmed.startsWith("classpath:") || trimmed.startsWith("file:")) {
            return trimmed;
        }
        return "file:" + trimmed;
    }
}
