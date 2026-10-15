package com.example.blog.controller;

import com.example.blog.common.Result;
import com.example.blog.security.BlogPrincipal;
import com.example.blog.service.BackupService;
import com.example.blog.service.OperationLogService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/backup")
public class BackupController {
    private final BackupService backupService;
    private final OperationLogService operationLogService;

    public BackupController(BackupService backupService, OperationLogService operationLogService) {
        this.backupService = backupService;
        this.operationLogService = operationLogService;
    }

    @PostMapping("/export")
    public ResponseEntity<StreamingResponseBody> export(@AuthenticationPrincipal BlogPrincipal principal,
                                                        @RequestBody(required = false) Map<String, String> payload) {
        String password = payload == null ? "" : payload.getOrDefault("password", "");
        BackupService.BackupArtifact artifact = backupService.createBackup(password);
        operationLogService.record(principal, "BACKUP", "SYSTEM", null,
                password.isBlank() ? "创建整站备份" : "创建加密整站备份");
        StreamingResponseBody body = output -> {
            try (InputStream input = Files.newInputStream(artifact.path())) {
                input.transferTo(output);
            } finally {
                Files.deleteIfExists(artifact.path());
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(artifact.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(artifact.filename(), java.nio.charset.StandardCharsets.UTF_8)
                        .build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(body);
    }

    @PostMapping("/uploads")
    public Result<Map<String, Object>> beginUpload(@RequestBody Map<String, Object> payload) {
        String filename = String.valueOf(payload.getOrDefault("filename", ""));
        long size = number(payload.get("size"), "备份文件大小").longValue();
        int totalChunks = number(payload.get("totalChunks"), "备份分片数量").intValue();
        return Result.ok(backupService.beginUpload(filename, size, totalChunks));
    }

    @PostMapping(value = "/uploads/{uploadId}/chunks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> appendChunk(@PathVariable String uploadId,
                                                   @RequestParam int index,
                                                   @RequestPart("file") MultipartFile file) {
        return Result.ok(backupService.appendChunk(uploadId, index, file));
    }

    @PostMapping("/uploads/{uploadId}/inspect")
    public Result<BackupService.BackupInspection> inspect(@PathVariable String uploadId,
                                                          @RequestBody(required = false) Map<String, String> payload) {
        String password = payload == null ? "" : payload.getOrDefault("password", "");
        return Result.ok(backupService.inspectUpload(uploadId, password));
    }

    @PostMapping("/uploads/{uploadId}/restore")
    public Result<BackupService.RestoreResult> restore(@AuthenticationPrincipal BlogPrincipal principal,
                                                       @PathVariable String uploadId,
                                                       @RequestBody Map<String, String> payload) {
        BackupService.RestoreResult result = backupService.restoreUpload(uploadId, payload.get("confirmation"));
        operationLogService.record(principal, "RESTORE", "SYSTEM", null,
                "恢复整站备份 v" + result.backupVersion() + "，数据 " + result.restoredRows() + " 条，文件 " + result.restoredFiles() + " 个");
        return Result.ok(result);
    }

    @DeleteMapping("/uploads/{uploadId}")
    public Result<Void> cancelUpload(@PathVariable String uploadId) {
        backupService.discardUpload(uploadId);
        return Result.ok();
    }

    private Number number(Object value, String label) {
        if (value instanceof Number number) return number;
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + "无效");
        }
    }
}
