package com.example.blog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Date;
import java.sql.ResultSetMetaData;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {
    private static final String FORMAT = "bokexitong-backup";
    private static final int FORMAT_VERSION = 1;
    private static final long MAX_ARCHIVE_BYTES = 20L * 1024 * 1024 * 1024;
    private static final long MAX_DATABASE_BYTES = 512L * 1024 * 1024;
    private static final long MAX_CHUNK_BYTES = 10L * 1024 * 1024;
    private static final int MAX_ARCHIVE_ENTRIES = 100_000;
    private static final long SESSION_TTL_MILLIS = 2L * 60 * 60 * 1000;
    private static final byte[] ENCRYPTED_MAGIC = "BOKEXITONG-BACKUP-2\n".getBytes(StandardCharsets.US_ASCII);
    private static final int PBKDF2_ITERATIONS = 210_000;
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final List<String> TABLES = List.of(
            "blog_user", "category", "tag", "article", "article_tag", "comment",
            "like_record", "favorite", "image_resource", "site_setting", "operation_log", "email_code"
    );
    private static final List<String> DELETE_ORDER = List.of(
            "email_code", "operation_log", "site_setting", "image_resource", "favorite", "like_record",
            "comment", "article_tag", "article", "tag", "category", "blog_user"
    );

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final SiteSettingService siteSettingService;
    private final Path uploadRoot;
    private final String appVersion;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ReentrantLock operationLock = new ReentrantLock();
    private final Map<String, UploadSession> uploads = new ConcurrentHashMap<>();

    public BackupService(JdbcTemplate jdbcTemplate,
                         ObjectMapper objectMapper,
                         PlatformTransactionManager transactionManager,
                         SiteSettingService siteSettingService,
                         @Value("${blog.upload-dir}") String uploadDir,
                         @Value("${blog.version:development}") String appVersion) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.siteSettingService = siteSettingService;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.appVersion = appVersion;
    }

    public BackupArtifact createBackup(String password) {
        if (!operationLock.tryLock()) {
            throw new IllegalArgumentException("系统正在执行其他备份或恢复任务，请稍后重试");
        }
        try {
            return createBackupUnlocked(password);
        } finally {
            operationLock.unlock();
        }
    }

    private BackupArtifact createBackupUnlocked(String password) {
        Path zip = null;
        Path output = null;
        try {
            zip = Files.createTempFile("bokexitong-backup-", ".zip");
            writeArchive(zip);
            String cleanPassword = password == null ? "" : password;
            if (!cleanPassword.isBlank() && cleanPassword.length() < 8) {
                throw new IllegalArgumentException("备份密码至少需要 8 位");
            }
            if (!cleanPassword.isBlank()) {
                output = Files.createTempFile("bokexitong-backup-", ".bkbak");
                encrypt(zip, output, cleanPassword);
            } else {
                output = Files.createTempFile("bokexitong-backup-", ".bkbak");
                Files.move(zip, output, StandardCopyOption.REPLACE_EXISTING);
                zip = null;
            }
            String filename = "bokexitong-" + FILE_TIME.format(LocalDateTime.now()) + ".bkbak";
            return new BackupArtifact(output, filename, Files.size(output));
        } catch (Exception ex) {
            deleteQuietly(output);
            throw backupError("创建整站备份失败", ex);
        } finally {
            deleteQuietly(zip);
        }
    }

    private void writeArchive(Path target) throws IOException {
        Path databaseFile = Files.createTempFile("bokexitong-database-", ".json");
        try {
            Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();
            Map<String, Long> tableRows = new LinkedHashMap<>();
            for (String table : TABLES) {
                List<Map<String, Object>> rows = normalizeRows(jdbcTemplate.queryForList("SELECT * FROM `" + table + "`"));
                tables.put(table, rows);
                tableRows.put(table, (long) rows.size());
            }
            objectMapper.writeValue(databaseFile.toFile(), Map.of("tables", tables));

            List<Path> uploadFiles = listUploadFiles();
            List<ArchiveEntryInfo> entries = new ArrayList<>();
            long uploadBytes = 0;
            try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(target)))) {
                entries.add(writeZipEntry(zip, "database.json", databaseFile));
                for (Path file : uploadFiles) {
                    String relative = uploadRoot.relativize(file).toString().replace('\\', '/');
                    ArchiveEntryInfo entry = writeZipEntry(zip, "uploads/" + relative, file);
                    entries.add(entry);
                    uploadBytes += entry.size();
                }
                BackupManifest manifest = new BackupManifest(
                        FORMAT,
                        FORMAT_VERSION,
                        appVersion,
                        Instant.now().toString(),
                        tableRows,
                        entries,
                        uploadFiles.size(),
                        uploadBytes
                );
                zip.putNextEntry(new ZipEntry("manifest.json"));
                zip.write(objectMapper.writeValueAsBytes(manifest));
                zip.closeEntry();
            }
        } finally {
            deleteQuietly(databaseFile);
        }
    }

    private List<Map<String, Object>> normalizeRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> normalized = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Map<String, Object> next = new LinkedHashMap<>();
            row.forEach((key, value) -> next.put(key, normalizeJdbcValue(value)));
            normalized.add(next);
        }
        return normalized;
    }

    private Object normalizeJdbcValue(Object value) {
        if (value instanceof Timestamp timestamp) return timestamp.toString();
        if (value instanceof Date date) return date.toLocalDate().toString();
        if (value instanceof Time time) return time.toLocalTime().toString();
        if (value instanceof byte[] bytes) return Map.of("$binary", Base64.getEncoder().encodeToString(bytes));
        return value;
    }

    private List<Path> listUploadFiles() throws IOException {
        if (!Files.exists(uploadRoot)) return List.of();
        try (var stream = Files.walk(uploadRoot)) {
            return stream.filter(path -> Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)).sorted().toList();
        }
    }

    private ArchiveEntryInfo writeZipEntry(ZipOutputStream zip, String name, Path source) throws IOException {
        MessageDigest digest = sha256Digest();
        long size = 0;
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0L);
        zip.putNextEntry(entry);
        byte[] buffer = new byte[64 * 1024];
        try (InputStream input = new BufferedInputStream(Files.newInputStream(source))) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                zip.write(buffer, 0, read);
                digest.update(buffer, 0, read);
                size += read;
            }
        }
        zip.closeEntry();
        return new ArchiveEntryInfo(name, size, hex(digest.digest()));
    }

    public Map<String, Object> beginUpload(String filename, long size, int totalChunks) {
        cleanupExpiredUploads();
        String cleanName = filename == null ? "" : filename.trim();
        if (!cleanName.toLowerCase(Locale.ROOT).endsWith(".bkbak")) {
            throw new IllegalArgumentException("请选择 .bkbak 整站备份文件");
        }
        if (size <= 0 || size > MAX_ARCHIVE_BYTES) {
            throw new IllegalArgumentException("备份文件大小无效或超过 20GB 上限");
        }
        if (totalChunks <= 0 || totalChunks > 10_000) {
            throw new IllegalArgumentException("备份分片数量无效");
        }
        try {
            String id = UUID.randomUUID().toString();
            Path file = Files.createTempFile("bokexitong-restore-", ".bkbak");
            uploads.put(id, new UploadSession(id, cleanName, size, totalChunks, file));
            return Map.of("uploadId", id, "chunkSize", 5L * 1024 * 1024);
        } catch (IOException ex) {
            throw backupError("无法创建恢复上传任务", ex);
        }
    }

    public Map<String, Object> appendChunk(String uploadId, int index, MultipartFile chunk) {
        UploadSession session = requireUpload(uploadId);
        synchronized (session) {
            if (session.inspection != null) throw new IllegalArgumentException("备份文件已经完成预检");
            if (index != session.nextChunk) throw new IllegalArgumentException("备份分片顺序不正确，请重新上传");
            if (chunk == null || chunk.isEmpty() || chunk.getSize() > MAX_CHUNK_BYTES) {
                throw new IllegalArgumentException("单个备份分片必须在 10MB 以内");
            }
            if (session.receivedBytes + chunk.getSize() > session.expectedBytes) {
                throw new IllegalArgumentException("上传数据超过备份文件声明大小");
            }
            try (InputStream input = chunk.getInputStream();
                 OutputStream output = Files.newOutputStream(session.file, StandardOpenOption.APPEND)) {
                input.transferTo(output);
            } catch (IOException ex) {
                throw backupError("备份分片写入失败", ex);
            }
            session.receivedBytes += chunk.getSize();
            session.nextChunk += 1;
            session.touchedAt = System.currentTimeMillis();
            int progress = (int) Math.min(100, Math.round(session.receivedBytes * 100.0 / session.expectedBytes));
            return Map.of("receivedBytes", session.receivedBytes, "nextChunk", session.nextChunk, "progress", progress);
        }
    }

    public BackupInspection inspectUpload(String uploadId, String password) {
        UploadSession session = requireUpload(uploadId);
        synchronized (session) {
            if (session.nextChunk != session.totalChunks || session.receivedBytes != session.expectedBytes) {
                throw new IllegalArgumentException("备份文件尚未上传完整");
            }
            cleanupInspection(session.inspection);
            session.inspection = inspectBackup(session.file, password);
            session.touchedAt = System.currentTimeMillis();
            return session.inspection.publicCopy();
        }
    }

    public RestoreResult restoreUpload(String uploadId, String confirmation) {
        if (!"确认恢复".equals(confirmation)) {
            throw new IllegalArgumentException("请输入“确认恢复”后再继续");
        }
        UploadSession session = requireUpload(uploadId);
        synchronized (session) {
            if (session.inspection == null) throw new IllegalArgumentException("请先完成备份文件预检");
        }
        if (!operationLock.tryLock()) {
            throw new IllegalArgumentException("系统正在执行其他备份或恢复任务，请稍后重试");
        }
        BackupArtifact rollback = null;
        Path previousUploads = null;
        boolean uploadsSwapped = false;
        try {
            rollback = createBackupUnlocked("");
            Path stagedUploads = stageUploads(session.inspection.root().resolve("uploads"));
            Path[] swapState = new Path[1];
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    restoreDatabase(session.inspection.root().resolve("database.json"));
                    try {
                        swapState[0] = swapUploads(stagedUploads);
                    } catch (IOException ex) {
                        throw backupError("替换上传文件失败，数据库恢复已撤销", ex);
                    }
                });
                previousUploads = swapState[0];
                uploadsSwapped = true;
            } catch (RuntimeException ex) {
                if (swapState[0] != null) restorePreviousUploads(swapState[0]);
                else deleteTreeQuietly(stagedUploads);
                throw ex;
            }
            siteSettingService.invalidateCache();
            String loginPath = siteSettingService.publicSettings().getOrDefault(SiteSettingService.ADMIN_LOGIN_PATH, "/admin/login");
            RestoreResult result = new RestoreResult(
                    session.inspection.manifest().appVersion(),
                    session.inspection.totalRows(),
                    session.inspection.manifest().uploadFiles(),
                    loginPath
            );
            discardUpload(uploadId);
            deleteTreeQuietly(previousUploads);
            return result;
        } catch (RuntimeException ex) {
            if (uploadsSwapped && previousUploads != null) restorePreviousUploads(previousUploads);
            throw ex;
        } finally {
            if (rollback != null) deleteQuietly(rollback.path());
            operationLock.unlock();
        }
    }

    public void discardUpload(String uploadId) {
        UploadSession session = uploads.remove(uploadId);
        if (session == null) return;
        cleanupInspection(session.inspection);
        deleteQuietly(session.file);
    }

    @PreDestroy
    public void cleanupTemporaryUploads() {
        List.copyOf(uploads.keySet()).forEach(this::discardUpload);
    }

    public BackupInspection inspectBackup(Path archive, String password) {
        Path decrypted = null;
        Path extraction = null;
        try {
            Path zip = archive;
            if (isEncrypted(archive)) {
                if (password == null || password.isBlank()) throw new IllegalArgumentException("这个备份已加密，请输入备份密码");
                decrypted = Files.createTempFile("bokexitong-decrypted-", ".zip");
                decrypt(archive, decrypted, password);
                zip = decrypted;
            }
            extraction = Files.createTempDirectory("bokexitong-inspect-");
            Map<String, ArchiveEntryInfo> actual = extractArchive(zip, extraction);
            Path manifestPath = extraction.resolve("manifest.json");
            Path databasePath = extraction.resolve("database.json");
            if (!Files.isRegularFile(manifestPath) || !Files.isRegularFile(databasePath)) {
                throw new IllegalArgumentException("备份包缺少 manifest.json 或 database.json");
            }
            BackupManifest manifest = objectMapper.readValue(manifestPath.toFile(), BackupManifest.class);
            validateManifest(manifest, actual);
            JsonNode database = objectMapper.readTree(databasePath.toFile());
            validateDatabase(database);
            long totalRows = manifest.tableRows().values().stream().mapToLong(Long::longValue).sum();
            return new BackupInspection(manifest, totalRows, Files.size(archive), isEncrypted(archive), extraction);
        } catch (IllegalArgumentException ex) {
            deleteTreeQuietly(extraction);
            throw ex;
        } catch (Exception ex) {
            deleteTreeQuietly(extraction);
            if (containsBadTag(ex)) throw new IllegalArgumentException("备份密码不正确或文件已经损坏");
            throw backupError("无法读取备份文件", ex);
        } finally {
            deleteQuietly(decrypted);
        }
    }

    public void cleanupInspection(BackupInspection inspection) {
        if (inspection != null) deleteTreeQuietly(inspection.root());
    }

    private Map<String, ArchiveEntryInfo> extractArchive(Path zipFile, Path root) throws IOException {
        Map<String, ArchiveEntryInfo> entries = new LinkedHashMap<>();
        long totalBytes = 0;
        int count = 0;
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipFile)))) {
            ZipEntry entry;
            byte[] buffer = new byte[64 * 1024];
            while ((entry = zip.getNextEntry()) != null) {
                count += 1;
                if (count > MAX_ARCHIVE_ENTRIES) throw new IllegalArgumentException("备份包文件数量超过安全上限");
                String name = normalizeEntryName(entry.getName());
                if (entries.containsKey(name)) throw new IllegalArgumentException("备份包包含重复文件：" + name);
                Path target = root.resolve(name).normalize();
                if (!target.startsWith(root)) throw new IllegalArgumentException("备份包包含不安全路径");
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    zip.closeEntry();
                    continue;
                }
                Files.createDirectories(target.getParent());
                MessageDigest digest = sha256Digest();
                long size = 0;
                try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(target, StandardOpenOption.CREATE_NEW))) {
                    int read;
                    while ((read = zip.read(buffer)) != -1) {
                        size += read;
                        totalBytes += read;
                        if (totalBytes > MAX_ARCHIVE_BYTES) throw new IllegalArgumentException("备份包解压后超过 20GB 安全上限");
                        if ("database.json".equals(name) && size > MAX_DATABASE_BYTES) {
                            throw new IllegalArgumentException("备份包数据库内容超过 512MB 安全上限");
                        }
                        output.write(buffer, 0, read);
                        digest.update(buffer, 0, read);
                    }
                }
                entries.put(name, new ArchiveEntryInfo(name, size, hex(digest.digest())));
                zip.closeEntry();
            }
        }
        return entries;
    }

    private String normalizeEntryName(String raw) {
        String name = raw == null ? "" : raw.replace('\\', '/');
        if (name.isBlank() || name.startsWith("/") || name.contains("../") || name.contains(":")) {
            throw new IllegalArgumentException("备份包包含不安全路径");
        }
        if (!("manifest.json".equals(name) || "database.json".equals(name) || name.startsWith("uploads/"))) {
            throw new IllegalArgumentException("备份包包含未知内容：" + name);
        }
        return name;
    }

    private void validateManifest(BackupManifest manifest, Map<String, ArchiveEntryInfo> actual) {
        if (manifest == null || !FORMAT.equals(manifest.format()) || manifest.formatVersion() != FORMAT_VERSION) {
            throw new IllegalArgumentException("不是受支持的博客整站备份格式");
        }
        if (manifest.tableRows() == null || manifest.entries() == null) {
            throw new IllegalArgumentException("备份清单内容不完整");
        }
        if (compareVersions(manifest.appVersion(), appVersion) > 0) {
            throw new IllegalArgumentException("备份来自更高版本 v" + manifest.appVersion() + "，请先升级当前博客");
        }
        Map<String, ArchiveEntryInfo> expected = new LinkedHashMap<>();
        for (ArchiveEntryInfo entry : manifest.entries()) {
            if (entry == null || expected.put(entry.path(), entry) != null) {
                throw new IllegalArgumentException("备份清单包含重复记录");
            }
        }
        actual.remove("manifest.json");
        if (!expected.keySet().equals(actual.keySet())) {
            throw new IllegalArgumentException("备份文件清单与实际内容不一致");
        }
        for (Map.Entry<String, ArchiveEntryInfo> item : expected.entrySet()) {
            ArchiveEntryInfo found = actual.get(item.getKey());
            if (found.size() != item.getValue().size() || !found.sha256().equalsIgnoreCase(item.getValue().sha256())) {
                throw new IllegalArgumentException("备份文件校验失败：" + item.getKey());
            }
        }
    }

    private void validateDatabase(JsonNode database) {
        JsonNode tables = database.path("tables");
        if (!tables.isObject()) throw new IllegalArgumentException("备份数据库结构无效");
        tables.fieldNames().forEachRemaining(name -> {
            if (!TABLES.contains(name)) throw new IllegalArgumentException("备份包含未知数据表：" + name);
            if (!tables.get(name).isArray()) throw new IllegalArgumentException("数据表内容无效：" + name);
        });
        if (!tables.has("blog_user") || !tables.has("site_setting")) {
            throw new IllegalArgumentException("备份缺少用户或站点设置数据");
        }
        boolean hasAdmin = false;
        for (JsonNode user : tables.path("blog_user")) {
            if ("ADMIN".equalsIgnoreCase(user.path("role").asText()) && user.path("status").asInt(1) == 1) {
                hasAdmin = true;
                break;
            }
        }
        if (!hasAdmin) throw new IllegalArgumentException("备份中没有可用的管理员账号，无法安全恢复");
    }

    private Path stageUploads(Path source) {
        try {
            Path parent = uploadRoot.getParent();
            if (parent == null) throw new IllegalArgumentException("上传目录不能位于文件系统根目录");
            Files.createDirectories(parent);
            Path staged = parent.resolve("." + uploadRoot.getFileName() + "-restore-" + UUID.randomUUID()).normalize();
            Files.createDirectories(staged);
            if (Files.exists(source)) copyTree(source, staged);
            return staged;
        } catch (IOException ex) {
            throw backupError("准备恢复上传文件失败", ex);
        }
    }

    private Path swapUploads(Path staged) throws IOException {
        Path parent = uploadRoot.getParent();
        Path previous = parent.resolve("." + uploadRoot.getFileName() + "-previous-" + UUID.randomUUID()).normalize();
        if (Files.exists(uploadRoot)) move(uploadRoot, previous);
        try {
            move(staged, uploadRoot);
            return previous;
        } catch (IOException ex) {
            if (Files.exists(previous)) move(previous, uploadRoot);
            throw ex;
        }
    }

    private void restorePreviousUploads(Path previous) {
        try {
            deleteTreeQuietly(uploadRoot);
            if (Files.exists(previous)) move(previous, uploadRoot);
        } catch (IOException ignored) {
            // The rollback archive still exists until this operation exits.
        }
    }

    private void restoreDatabase(Path databaseFile) {
        try {
            JsonNode tables = objectMapper.readTree(databaseFile.toFile()).path("tables");
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
            try {
                for (String table : DELETE_ORDER) jdbcTemplate.update("DELETE FROM `" + table + "`");
                for (String table : TABLES) restoreTable(table, tables.path(table));
                jdbcTemplate.update("INSERT INTO site_setting(setting_key, setting_value, description) VALUES (?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = CURRENT_TIMESTAMP",
                        SiteSettingService.INSTALLATION_COMPLETE, "true", "安装完成标记");
            } finally {
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
            }
        } catch (IOException ex) {
            throw backupError("读取数据库备份失败", ex);
        }
    }

    private void restoreTable(String table, JsonNode rows) {
        if (!rows.isArray() || rows.isEmpty()) return;
        Set<String> allowedColumns = currentColumns(table);
        List<String> columns = new ArrayList<>();
        rows.get(0).fieldNames().forEachRemaining(columns::add);
        if (columns.isEmpty() || !allowedColumns.containsAll(columns)) {
            throw new IllegalArgumentException("备份表字段与当前版本不兼容：" + table);
        }
        for (JsonNode row : rows) {
            Set<String> rowColumns = new LinkedHashSet<>();
            row.fieldNames().forEachRemaining(rowColumns::add);
            if (!rowColumns.equals(new LinkedHashSet<>(columns))) {
                throw new IllegalArgumentException("备份表字段不一致：" + table);
            }
        }
        String names = columns.stream().map(name -> "`" + name + "`").reduce((a, b) -> a + "," + b).orElseThrow();
        String placeholders = String.join(",", java.util.Collections.nCopies(columns.size(), "?"));
        String sql = "INSERT INTO `" + table + "` (" + names + ") VALUES (" + placeholders + ")";
        List<Object[]> batch = new ArrayList<>();
        for (JsonNode row : rows) {
            Object[] values = new Object[columns.size()];
            for (int i = 0; i < columns.size(); i++) values[i] = jsonValue(row.get(columns.get(i)));
            batch.add(values);
            if (batch.size() == 500) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) jdbcTemplate.batchUpdate(sql, batch);
    }

    private Set<String> currentColumns(String table) {
        return jdbcTemplate.query("SELECT * FROM `" + table + "` WHERE 1=0", resultSet -> {
            ResultSetMetaData meta = resultSet.getMetaData();
            Set<String> columns = new LinkedHashSet<>();
            for (int index = 1; index <= meta.getColumnCount(); index++) columns.add(meta.getColumnLabel(index));
            return columns;
        });
    }

    private Object jsonValue(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isTextual()) return node.textValue();
        if (node.isIntegralNumber()) return node.longValue();
        if (node.isFloatingPointNumber()) return node.decimalValue();
        if (node.isBoolean()) return node.booleanValue();
        if (node.isObject() && node.has("$binary")) return Base64.getDecoder().decode(node.get("$binary").asText());
        return node.toString();
    }

    private void copyTree(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target);
        }
    }

    private void encrypt(Path input, Path output, String password) throws Exception {
        byte[] salt = new byte[16];
        byte[] iv = new byte[12];
        secureRandom.nextBytes(salt);
        secureRandom.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), new GCMParameterSpec(128, iv));
        try (OutputStream raw = new BufferedOutputStream(Files.newOutputStream(output))) {
            raw.write(ENCRYPTED_MAGIC);
            raw.write(salt);
            raw.write(iv);
            try (CipherOutputStream encrypted = new CipherOutputStream(raw, cipher);
                 InputStream source = new BufferedInputStream(Files.newInputStream(input))) {
                source.transferTo(encrypted);
            }
        }
    }

    private void decrypt(Path input, Path output, String password) throws Exception {
        try (InputStream raw = new BufferedInputStream(Files.newInputStream(input))) {
            byte[] magic = raw.readNBytes(ENCRYPTED_MAGIC.length);
            if (!Arrays.equals(magic, ENCRYPTED_MAGIC)) throw new IllegalArgumentException("备份加密头无效");
            byte[] salt = raw.readNBytes(16);
            byte[] iv = raw.readNBytes(12);
            if (salt.length != 16 || iv.length != 12) throw new IllegalArgumentException("加密备份文件不完整");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(password, salt), new GCMParameterSpec(128, iv));
            try (CipherInputStream decrypted = new CipherInputStream(raw, cipher);
                 OutputStream target = new BufferedOutputStream(Files.newOutputStream(output))) {
                decrypted.transferTo(target);
            }
        }
    }

    private SecretKey key(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256);
        try {
            byte[] encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return new SecretKeySpec(encoded, "AES");
        } finally {
            spec.clearPassword();
        }
    }

    private boolean isEncrypted(Path archive) throws IOException {
        try (InputStream input = Files.newInputStream(archive)) {
            return Arrays.equals(input.readNBytes(ENCRYPTED_MAGIC.length), ENCRYPTED_MAGIC);
        }
    }

    private boolean containsBadTag(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof AEADBadTagException) return true;
            current = current.getCause();
        }
        return false;
    }

    private UploadSession requireUpload(String id) {
        UploadSession session = uploads.get(id);
        if (session == null || System.currentTimeMillis() - session.touchedAt > SESSION_TTL_MILLIS) {
            discardUpload(id);
            throw new IllegalArgumentException("恢复上传任务已失效，请重新选择备份文件");
        }
        return session;
    }

    private void cleanupExpiredUploads() {
        long now = System.currentTimeMillis();
        List<String> expired = uploads.values().stream()
                .filter(session -> now - session.touchedAt > SESSION_TTL_MILLIS)
                .map(session -> session.id)
                .toList();
        expired.forEach(this::discardUpload);
    }

    private int compareVersions(String left, String right) {
        int[] a = versionParts(left);
        int[] b = versionParts(right);
        for (int index = 0; index < 3; index++) {
            if (a[index] != b[index]) return Integer.compare(a[index], b[index]);
        }
        return 0;
    }

    private int[] versionParts(String version) {
        String[] parts = String.valueOf(version).replaceFirst("^[vV]", "").split("\\.");
        int[] values = new int[3];
        for (int index = 0; index < Math.min(3, parts.length); index++) {
            try {
                values[index] = Integer.parseInt(parts[index].replaceAll("[^0-9].*$", ""));
            } catch (NumberFormatException ignored) {
                values[index] = 0;
            }
        }
        return values;
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception ex) {
            throw new IllegalStateException("当前 Java 环境不支持 SHA-256", ex);
        }
    }

    private String hex(byte[] value) {
        StringBuilder output = new StringBuilder(value.length * 2);
        for (byte item : value) output.append(String.format("%02x", item));
        return output.toString();
    }

    private IllegalArgumentException backupError(String message, Exception ex) {
        return new IllegalArgumentException(message + "：" + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()), ex);
    }

    private void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private void deleteTreeQuietly(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(this::deleteQuietly);
        } catch (IOException ignored) {
        }
    }

    public record ArchiveEntryInfo(String path, long size, String sha256) {}

    public record BackupManifest(String format, int formatVersion, String appVersion, String createdAt,
                                 Map<String, Long> tableRows, List<ArchiveEntryInfo> entries,
                                 long uploadFiles, long uploadBytes) {}

    public record BackupArtifact(Path path, String filename, long size) {}

    public record BackupInspection(BackupManifest manifest, long totalRows, long archiveBytes,
                                   boolean encrypted, Path root) {
        public BackupInspection publicCopy() {
            return new BackupInspection(manifest, totalRows, archiveBytes, encrypted, null);
        }
    }

    public record RestoreResult(String backupVersion, long restoredRows, long restoredFiles, String adminLoginPath) {}

    private static final class UploadSession {
        private final String id;
        private final String filename;
        private final long expectedBytes;
        private final int totalChunks;
        private final Path file;
        private int nextChunk;
        private long receivedBytes;
        private long touchedAt = System.currentTimeMillis();
        private BackupInspection inspection;

        private UploadSession(String id, String filename, long expectedBytes, int totalChunks, Path file) {
            this.id = id;
            this.filename = filename;
            this.expectedBytes = expectedBytes;
            this.totalChunks = totalChunks;
            this.file = file;
        }
    }
}
