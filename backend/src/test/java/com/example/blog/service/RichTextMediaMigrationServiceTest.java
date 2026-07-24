package com.example.blog.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RichTextMediaMigrationServiceTest {
    private static final byte[] PNG = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
    };
    private static final byte[] JPEG = new byte[] {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46
    };

    @TempDir
    Path uploadDir;

    @Test
    void migrateEmbeddedImages_shouldStoreValidRasterImagesAndPreserveUploadedUrls() throws Exception {
        RichTextMediaMigrationService service = service();
        String html = "<p><img src=\"data:image/png;base64," + encoded(PNG) + "\"></p>"
                + "<img src=\"/api/uploads/existing.png\">"
                + "<img src=\"data:image/jpeg;base64," + encoded(JPEG) + "\">";

        String migrated = service.migrateEmbeddedImages(html, "HTML");

        assertFalse(migrated.contains("data:image"));
        assertTrue(migrated.contains("/api/uploads/existing.png"));
        assertTrue(migrated.contains("/api/uploads/"));
        assertEquals(2, fileCount());
    }

    @Test
    void migrateEmbeddedImages_shouldReuseFileForRepeatedDataUrl() throws Exception {
        RichTextMediaMigrationService service = service();
        String dataUrl = "data:image/png;base64," + encoded(PNG);

        String migrated = service.migrateEmbeddedImages(
                "<img src=\"" + dataUrl + "\"><img src=\"" + dataUrl + "\">",
                "HTML"
        );

        assertFalse(migrated.contains("data:image"));
        assertEquals(1, fileCount());
    }

    @Test
    void migrateEmbeddedImages_shouldAcceptLegacyJpgMimeType() throws Exception {
        RichTextMediaMigrationService service = service();

        String migrated = service.migrateEmbeddedImages(
                "<img src=\"data:image/jpg;base64," + encoded(JPEG) + "\">",
                "HTML"
        );

        assertFalse(migrated.contains("data:image"));
        assertEquals(1, fileCount());
    }

    @Test
    void migrateEmbeddedImages_shouldAbortAndCleanCreatedFilesWhenAnyImageIsMalformed() throws Exception {
        RichTextMediaMigrationService service = service();
        String html = "<img src=\"data:image/png;base64," + encoded(PNG) + "\">"
                + "<img src=\"data:image/png;base64,not-valid-base64!\">";

        assertThrows(IllegalArgumentException.class, () -> service.migrateEmbeddedImages(html, "HTML"));
        assertEquals(0, fileCount());
    }

    @Test
    void migrateEmbeddedImages_shouldRejectSvgDataUrls() throws Exception {
        RichTextMediaMigrationService service = service();
        String svg = Base64.getEncoder().encodeToString("<svg></svg>".getBytes());

        assertThrows(IllegalArgumentException.class,
                () -> service.migrateEmbeddedImages("<img src=\"data:image/svg+xml;base64," + svg + "\">", "HTML"));
        assertEquals(0, fileCount());
    }

    @Test
    void migrateEmbeddedImages_shouldLeaveMarkdownUntouched() {
        RichTextMediaMigrationService service = service();
        String markdown = "![image](data:image/png;base64," + encoded(PNG) + ")";
        assertEquals(markdown, service.migrateEmbeddedImages(markdown, "MARKDOWN"));
    }

    private RichTextMediaMigrationService service() {
        return new RichTextMediaMigrationService(new UploadStorageService(uploadDir.toString()));
    }

    private String encoded(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private long fileCount() throws Exception {
        try (var paths = Files.walk(uploadDir)) {
            return paths.filter(Files::isRegularFile).count();
        }
    }
}
