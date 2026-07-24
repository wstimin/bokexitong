package com.example.blog.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadStorageService {
    public static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

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
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/jpg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    private final Path uploadRoot;

    public UploadStorageService(@Value("${blog.upload-dir}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public StoredUpload store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("单个文件不能超过 50MB");
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = getExtension(original);
        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_EXTENSIONS.contains(extension) || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("不支持的文件类型");
        }

        byte[] header = readHeader(file, 16);
        validateFileSignature(header, file.getSize(), extension, contentType);
        Path target = createTarget(extension);
        try {
            file.transferTo(target);
        } catch (IOException | RuntimeException ex) {
            deleteQuietly(target);
            throw ex;
        }
        return storedUpload(target, original, contentType);
    }

    public StoredUpload storeEmbeddedImage(byte[] bytes, String contentType) throws IOException {
        String normalizedType = normalizeContentType(contentType);
        String extension = IMAGE_EXTENSIONS.get(normalizedType);
        if (extension == null) {
            throw new IllegalArgumentException("旧文章中包含不支持的内嵌图片格式");
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("旧文章中的内嵌图片内容为空");
        }
        if (bytes.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("旧文章中的单张内嵌图片不能超过 50MB");
        }

        validateFileSignature(bytes, bytes.length, extension, normalizedType);
        Path target = createTarget(extension);
        try {
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException ex) {
            deleteQuietly(target);
            throw ex;
        }
        return storedUpload(target, "embedded-image" + extension, normalizedType);
    }

    public void deleteQuietly(Path path) {
        if (path == null) return;
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(uploadRoot)) return;
        try {
            Files.deleteIfExists(normalized);
        } catch (IOException ignored) {
            // Best-effort cleanup for a failed or rolled-back article save.
        }
    }

    private Path createTarget(String extension) throws IOException {
        LocalDate today = LocalDate.now();
        Path dir = uploadRoot.resolve(String.valueOf(today.getYear()))
                .resolve(String.format("%02d", today.getMonthValue()))
                .resolve(String.format("%02d", today.getDayOfMonth()))
                .normalize();
        if (!dir.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("上传目录不合法");
        }
        Files.createDirectories(dir);
        Path target = dir.resolve(UUID.randomUUID() + extension).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("文件名不合法");
        }
        return target;
    }

    private StoredUpload storedUpload(Path target, String originalName, String contentType) {
        String url = "/api/uploads/" + uploadRoot.relativize(target).toString().replace('\\', '/');
        return new StoredUpload(url, originalName, contentType, target);
    }

    private void validateFileSignature(byte[] header, long size, String extension, String contentType) {
        if (contentType.startsWith("image/")) {
            boolean validImage = (Set.of(".jpg", ".jpeg").contains(extension) && startsWith(header, 0xFF, 0xD8, 0xFF))
                    || (".png".equals(extension) && startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
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
        if ((".mp4".equals(extension) || ".webm".equals(extension)) && size < 16) {
            throw new IllegalArgumentException("视频文件内容不合法");
        }
    }

    private byte[] readHeader(MultipartFile file, int size) throws IOException {
        byte[] header = new byte[size];
        try (InputStream input = file.getInputStream()) {
            int read = input.read(header);
            if (read <= 0) return new byte[0];
            if (read == size) return header;
            byte[] actual = new byte[read];
            System.arraycopy(header, 0, actual, 0, read);
            return actual;
        }
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

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            throw new IllegalArgumentException("文件缺少扩展名");
        }
        return filename.substring(dot).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    public record StoredUpload(String url, String originalName, String contentType, Path path) {
    }
}
