package com.example.blog.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RichTextSanitizerTest {

    @Test
    void sanitize_shouldKeepQuillFormattingAndUploadedMedia() {
        String html = "<p class=\"ql-align-center\" style=\"font-size: 20px; color: #123456\">正文</p>"
                + "<img src=\"/api/uploads/a.png\" alt=\"图片\">"
                + "<video src=\"/api/uploads/a.mp4\" controls preload=\"metadata\"></video>";

        String sanitized = RichTextSanitizer.sanitize(html, "HTML");

        assertTrue(sanitized.contains("ql-align-center"));
        assertTrue(sanitized.contains("font-size: 20px"));
        assertTrue(sanitized.contains("/api/uploads/a.png"));
        assertTrue(sanitized.contains("/api/uploads/a.mp4"));
    }

    @Test
    void sanitize_shouldRemoveExecutableHtmlAndUnsafeStyles() {
        String html = "<script>alert(1)</script><p onclick=\"alert(1)\" style=\"background-image:url(javascript:alert(1)); color:red\">正文</p>"
                + "<a href=\"javascript:alert(1)\">危险链接</a>";

        String sanitized = RichTextSanitizer.sanitize(html, "HTML");

        assertFalse(sanitized.contains("script"));
        assertFalse(sanitized.contains("onclick"));
        assertFalse(sanitized.contains("javascript:"));
        assertFalse(sanitized.contains("background-image"));
        assertTrue(sanitized.contains("color:red"));
    }

    @Test
    void sanitize_shouldKeepSafeLinksAndRemoveUnsafeMediaUrls() {
        String html = "<a href=\"https://example.com\" target=\"_blank\">外部</a>"
                + "<a href=\"/article/1\">站内</a>"
                + "<img src=\"data:image/svg+xml,unsafe\">";

        String sanitized = RichTextSanitizer.sanitize(html, "HTML");

        assertTrue(sanitized.contains("https://example.com"));
        assertTrue(sanitized.contains("rel=\"noopener noreferrer\""));
        assertTrue(sanitized.contains("/article/1"));
        assertFalse(sanitized.contains("data:image"));
    }

    @Test
    void sanitize_shouldLeaveMarkdownSourceUntouched() {
        String markdown = "[链接](https://example.com) <script>legacy</script>";
        assertTrue(RichTextSanitizer.sanitize(markdown, "MARKDOWN").equals(markdown));
    }
}
