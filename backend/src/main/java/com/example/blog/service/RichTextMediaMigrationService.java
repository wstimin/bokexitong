package com.example.blog.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RichTextMediaMigrationService {
    private static final Pattern DATA_IMAGE_PATTERN = Pattern.compile(
            "^data:([^;,]+);base64,(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final int MAX_ENCODED_IMAGE_LENGTH = (int) ((UploadStorageService.MAX_FILE_SIZE * 4 / 3) + 16);

    private final UploadStorageService uploadStorageService;

    public RichTextMediaMigrationService(UploadStorageService uploadStorageService) {
        this.uploadStorageService = uploadStorageService;
    }

    public String migrateEmbeddedImages(String content, String contentType) {
        if (content == null || !isHtml(contentType) || !containsDataImage(content)) {
            return content;
        }

        Document document = Jsoup.parseBodyFragment(content);
        document.outputSettings().prettyPrint(false);
        List<Path> createdFiles = new ArrayList<>();
        Map<String, UploadStorageService.StoredUpload> migratedImages = new HashMap<>();
        try {
            for (Element image : document.select("img[src]")) {
                String source = image.attr("src").trim();
                if (!source.toLowerCase(Locale.ROOT).startsWith("data:image/")) continue;
                UploadStorageService.StoredUpload stored = migratedImages.get(source);
                if (stored == null) {
                    stored = migrateImage(source);
                    migratedImages.put(source, stored);
                    createdFiles.add(stored.path());
                }
                image.attr("src", stored.url());
            }
        } catch (RuntimeException | IOException ex) {
            cleanup(createdFiles);
            if (ex instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalArgumentException("旧文章内嵌图片迁移失败，请检查 uploads 目录写入权限", ex);
        }

        registerRollbackCleanup(createdFiles);
        return document.body().html();
    }

    private UploadStorageService.StoredUpload migrateImage(String dataUrl) throws IOException {
        Matcher matcher = DATA_IMAGE_PATTERN.matcher(dataUrl == null ? "" : dataUrl.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("旧文章中存在无法识别的内嵌图片，文章未保存");
        }

        String contentType = matcher.group(1).trim().toLowerCase(Locale.ROOT);
        String payload = matcher.group(2);
        if (payload.length() > MAX_ENCODED_IMAGE_LENGTH) {
            throw new IllegalArgumentException("旧文章中的单张内嵌图片不能超过 50MB");
        }

        final byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("旧文章中存在损坏的 Base64 图片，文章未保存", ex);
        }
        return uploadStorageService.storeEmbeddedImage(bytes, contentType);
    }

    private void registerRollbackCleanup(List<Path> createdFiles) {
        if (createdFiles.isEmpty() || !TransactionSynchronizationManager.isSynchronizationActive()) return;
        List<Path> rollbackFiles = List.copyOf(createdFiles);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) cleanup(rollbackFiles);
            }
        });
    }

    private void cleanup(List<Path> files) {
        files.forEach(uploadStorageService::deleteQuietly);
    }

    private boolean containsDataImage(String content) {
        return content.toLowerCase(Locale.ROOT).contains("data:image/");
    }

    private boolean isHtml(String contentType) {
        String normalized = contentType == null ? "" : contentType.trim().toUpperCase(Locale.ROOT);
        return "HTML".equals(normalized) || "RICH_TEXT".equals(normalized);
    }
}
