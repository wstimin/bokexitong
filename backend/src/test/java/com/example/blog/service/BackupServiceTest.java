package com.example.blog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackupServiceTest {
    @TempDir Path tempDir;

    @Test
    void encryptedBackupKeepsUploadsAndRejectsTheWrongPassword() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString())).thenAnswer(invocation -> {
            Object sql = invocation.getArgument(0);
            return sql.toString().contains("blog_user")
                    ? List.of(Map.of("id", 1L, "username", "admin", "role", "ADMIN", "status", 1))
                    : List.of();
        });
        Path uploads = tempDir.resolve("uploads");
        Path image = uploads.resolve("2026/10/15/avatar.png");
        Files.createDirectories(image.getParent());
        Files.writeString(image, "image-content", StandardCharsets.UTF_8);

        BackupService service = new BackupService(
                jdbcTemplate,
                new ObjectMapper(),
                mock(PlatformTransactionManager.class),
                mock(SiteSettingService.class),
                uploads.toString(),
                "1.1.33"
        );

        BackupService.BackupArtifact artifact = service.createBackup("safe-password");
        try {
            BackupService.BackupInspection inspection = service.inspectBackup(artifact.path(), "safe-password");
            try {
                assertTrue(inspection.encrypted());
                assertEquals(1, inspection.manifest().uploadFiles());
                assertEquals(Files.size(image), inspection.manifest().uploadBytes());
                assertEquals("image-content", Files.readString(
                        inspection.root().resolve("uploads/2026/10/15/avatar.png"),
                        StandardCharsets.UTF_8
                ));
            } finally {
                service.cleanupInspection(inspection);
            }
            assertThrows(IllegalArgumentException.class, () -> service.inspectBackup(artifact.path(), "wrong-password"));
        } finally {
            Files.deleteIfExists(artifact.path());
        }
    }

    @Test
    void backupPasswordMustBeLongEnough() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString())).thenAnswer(invocation -> {
            Object sql = invocation.getArgument(0);
            return sql.toString().contains("blog_user")
                    ? List.of(Map.of("id", 1L, "username", "admin", "role", "ADMIN", "status", 1))
                    : List.of();
        });
        BackupService service = new BackupService(
                jdbcTemplate,
                new ObjectMapper(),
                mock(PlatformTransactionManager.class),
                mock(SiteSettingService.class),
                tempDir.resolve("uploads").toString(),
                "1.1.33"
        );

        assertThrows(IllegalArgumentException.class, () -> service.createBackup("short"));
    }

    @Test
    void restoreUsesTheValidatedArchiveAndReplacesLocalUploads() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString())).thenAnswer(invocation -> {
            Object sql = invocation.getArgument(0);
            return sql.toString().contains("blog_user")
                    ? List.of(Map.of(
                            "id", 1L,
                            "username", "admin",
                            "nickname", "Admin",
                            "password", "{noop}safe-password",
                            "role", "ADMIN",
                            "status", 1
                    ))
                    : List.of();
        });
        doReturn(new LinkedHashSet<>(Set.of("id", "username", "nickname", "password", "role", "status")))
                .when(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class));
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        SiteSettingService siteSettings = mock(SiteSettingService.class);
        when(siteSettings.publicSettings()).thenReturn(Map.of(SiteSettingService.ADMIN_LOGIN_PATH, "/admin/login"));

        Path uploads = tempDir.resolve("restore-uploads");
        Path image = uploads.resolve("2026/10/15/avatar.png");
        Files.createDirectories(image.getParent());
        Files.writeString(image, "backup-image", StandardCharsets.UTF_8);
        BackupService service = new BackupService(
                jdbcTemplate,
                new ObjectMapper(),
                transactionManager,
                siteSettings,
                uploads.toString(),
                "1.1.33"
        );

        BackupService.BackupArtifact artifact = service.createBackup("");
        try {
            Files.writeString(image, "current-image", StandardCharsets.UTF_8);
            byte[] bytes = Files.readAllBytes(artifact.path());
            Map<String, Object> upload = service.beginUpload("site.bkbak", bytes.length, 1);
            String uploadId = String.valueOf(upload.get("uploadId"));
            service.appendChunk(uploadId, 0, new MockMultipartFile("file", "backup.part-0", "application/octet-stream", bytes));
            service.inspectUpload(uploadId, "");
            BackupService.RestoreResult result = service.restoreUpload(uploadId, "确认恢复");

            assertEquals("backup-image", Files.readString(image, StandardCharsets.UTF_8));
            assertEquals(1, result.restoredFiles());
            assertEquals("/admin/login", result.adminLoginPath());
        } finally {
            Files.deleteIfExists(artifact.path());
        }
    }
}
