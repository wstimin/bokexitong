package com.example.blog.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UpdateService {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Path requestPath;
    private final Path statusPath;
    private final String currentVersion;
    private final String buildTime;
    private final String manifestUrl;
    private final String mode;
    private final boolean enabled;

    public UpdateService(ObjectMapper objectMapper,
                         @Value("${blog.version:1.1.15}") String currentVersion,
                         @Value("${blog.build-time:development}") String buildTime,
                         @Value("${blog.update.manifest-url:}") String manifestUrl,
                         @Value("${blog.update.request-path:./config/update-request.json}") String requestPath,
                         @Value("${blog.update.status-path:./config/update-status.json}") String statusPath,
                         @Value("${blog.update.mode:disabled}") String mode,
                         @Value("${blog.update.enabled:true}") boolean enabled) {
        this.objectMapper = objectMapper;
        this.currentVersion = currentVersion;
        this.buildTime = buildTime;
        this.manifestUrl = manifestUrl == null ? "" : manifestUrl.trim();
        this.requestPath = Path.of(requestPath).toAbsolutePath().normalize();
        this.statusPath = Path.of(statusPath).toAbsolutePath().normalize();
        this.mode = mode == null ? "disabled" : mode.trim().toLowerCase();
        this.enabled = enabled;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Map<String, Object> status() {
        Map<String, Object> data = baseInfo();
        data.put("state", "IDLE");
        data.put("message", agentAvailable() ? "更新服务已就绪" : "当前部署尚未启用更新代理");
        if (Files.isRegularFile(statusPath)) {
            try {
                Map<String, Object> savedStatus = objectMapper.readValue(statusPath.toFile(), new TypeReference<>() {});
                data.putAll(savedStatus);
                String targetVersion = stringValue(savedStatus.get("targetVersion"));
                if (!targetVersion.isBlank() && compareVersions(currentVersion, targetVersion) >= 0
                        && !"FAILED".equals(savedStatus.get("state"))) {
                    data.put("state", "COMPLETED");
                    data.put("message", "系统已成功更新到 v" + currentVersion);
                }
            } catch (IOException ignored) {
                data.put("state", "UNKNOWN");
                data.put("message", "更新状态文件暂时不可读");
            }
        } else if (Files.isRegularFile(requestPath)) {
            data.put("state", "REQUESTED");
            data.put("message", "更新任务已提交，等待更新代理接管");
        }
        return data;
    }

    public Map<String, Object> check() {
        Map<String, Object> data = baseInfo();
        if (!enabled) {
            data.put("available", false);
            data.put("message", "自动更新已被管理员关闭");
            return data;
        }
        if (manifestUrl.isBlank()) {
            data.put("available", false);
            data.put("message", "尚未配置更新清单地址");
            return data;
        }
        Map<String, Object> manifest = fetchManifest();
        String latestVersion = stringValue(manifest.get("version"));
        data.put("latestVersion", latestVersion);
        data.put("releaseDate", stringValue(manifest.get("releaseDate")));
        data.put("notes", stringValue(manifest.get("notes")));
        data.put("available", compareVersions(latestVersion, currentVersion) > 0);
        data.put("agentAvailable", agentAvailable());
        data.put("message", Boolean.TRUE.equals(data.get("available")) ? "发现新版本" : "当前已是最新版本");
        return data;
    }

    public synchronized Map<String, Object> requestUpdate() {
        if (!agentAvailable()) {
            throw new IllegalArgumentException("当前部署未启用更新代理，请先按部署文档配置更新服务");
        }
        Map<String, Object> manifest = fetchManifest();
        String latestVersion = stringValue(manifest.get("version"));
        if (compareVersions(latestVersion, currentVersion) <= 0) {
            throw new IllegalArgumentException("当前已是最新版本");
        }
        Map<String, String> packageInfo = packageInfo(manifest);
        String packageUrl = packageInfo.getOrDefault("url", "");
        validatePackageUrl(packageUrl);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("taskId", UUID.randomUUID().toString());
        request.put("requestedAt", Instant.now().toString());
        request.put("fromVersion", currentVersion);
        request.put("toVersion", latestVersion);
        request.put("mode", mode);
        request.put("packageUrl", packageUrl);
        request.put("sha256", packageInfo.getOrDefault("sha256", stringValue(manifest.get("sha256"))));
        writeAtomically(requestPath, request);
        try {
            Files.deleteIfExists(statusPath);
        } catch (IOException ignored) {
            // A stale status never blocks a valid update request.
        }

        if ("web".equals(mode)) {
            Thread restartThread = new Thread(() -> {
                try {
                    Thread.sleep(1_500L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                System.exit(42);
            }, "blog-update-handoff");
            restartThread.setDaemon(true);
            restartThread.start();
        }
        return request;
    }

    private Map<String, Object> baseInfo() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("currentVersion", currentVersion);
        data.put("buildTime", buildTime);
        data.put("mode", mode);
        data.put("enabled", enabled);
        data.put("agentAvailable", agentAvailable());
        return data;
    }

    private boolean agentAvailable() {
        return enabled && ("docker".equals(mode) || "web".equals(mode));
    }

    private Map<String, Object> fetchManifest() {
        try {
            URI uri = URI.create(manifestUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException("更新清单必须使用 HTTPS");
            }
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(12))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Bokexitong-Updater/" + currentVersion)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException("更新服务器返回异常状态：" + response.statusCode());
            }
            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("无法连接更新服务器，请稍后重试");
        }
    }

    private Map<String, String> packageInfo(Map<String, Object> manifest) {
        Object packages = manifest.get("packages");
        if (packages instanceof Map<?, ?> packageMap) {
            Object target = packageMap.get("web".equals(mode) ? "web" : "docker");
            if (target instanceof Map<?, ?> targetMap) {
                return Map.of(
                        "url", stringValue(targetMap.get("url")),
                        "sha256", stringValue(targetMap.get("sha256"))
                );
            }
            return Map.of("url", stringValue(target), "sha256", "");
        }
        return Map.of("url", stringValue(manifest.get("packageUrl")), "sha256", stringValue(manifest.get("sha256")));
    }

    private void validatePackageUrl(String packageUrl) {
        if (packageUrl.isBlank()) {
            throw new IllegalArgumentException("更新清单缺少当前部署方式的安装包");
        }
        URI uri = URI.create(packageUrl);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException("更新包地址必须是有效的 HTTPS 地址");
        }
    }

    private void writeAtomically(Path target, Map<String, Object> value) {
        try {
            Path parent = target.getParent();
            if (parent != null) Files.createDirectories(parent);
            Path temp = target.resolveSibling(target.getFileName() + ".tmp");
            objectMapper.writeValue(temp.toFile(), value);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("无法创建更新任务，请检查 config 目录写入权限");
        }
    }

    private int compareVersions(String left, String right) {
        String[] leftParts = normalizeVersion(left).split("\\.");
        String[] rightParts = normalizeVersion(right).split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < length; i++) {
            int a = i < leftParts.length ? parsePart(leftParts[i]) : 0;
            int b = i < rightParts.length ? parsePart(rightParts[i]) : 0;
            if (a != b) return Integer.compare(a, b);
        }
        return 0;
    }

    private String normalizeVersion(String value) {
        String normalized = stringValue(value).trim().replaceFirst("^[vV]", "");
        int suffix = normalized.indexOf('-');
        return suffix >= 0 ? normalized.substring(0, suffix) : normalized;
    }

    private int parsePart(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9].*$", ""));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
