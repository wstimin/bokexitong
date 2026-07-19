package com.example.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Path;

@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {
    private final String uploadDir;
    private final String webRoot;

    public UploadResourceConfig(@Value("${blog.upload-dir}") String uploadDir,
                                @Value("${blog.web-root:file:./frontend/dist/}") String webRoot) {
        this.uploadDir = uploadDir;
        this.webRoot = normalizeLocation(webRoot);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/api/uploads/**").addResourceLocations(location);
        registry.addResourceHandler("/assets/**").addResourceLocations(webRoot + "assets/");
        registry.addResourceHandler("/favicon.ico").addResourceLocations(webRoot);
        registry.addResourceHandler("/**")
                .addResourceLocations(webRoot)
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }

    private String normalizeLocation(String location) {
        String trimmed = location == null ? "" : location.trim();
        if (trimmed.isEmpty()) {
            return "file:./frontend/dist/";
        }
        if (!trimmed.endsWith("/")) {
            trimmed = trimmed + "/";
        }
        if (trimmed.startsWith("classpath:") || trimmed.startsWith("file:")) {
            return trimmed;
        }
        return "file:" + trimmed;
    }

    private static final class SpaResourceResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource resource = super.getResource(resourcePath, location);
            if (resource != null) {
                return resource;
            }
            if (resourcePath.startsWith("api/") || resourcePath.startsWith("assets/") || resourcePath.contains(".")) {
                return null;
            }
            Resource index = location.createRelative("index.html");
            return index.exists() && index.isReadable() ? index : null;
        }
    }
}
