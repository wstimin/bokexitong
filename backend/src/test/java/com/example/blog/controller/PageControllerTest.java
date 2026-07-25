package com.example.blog.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageControllerTest {
    @TempDir
    Path webRoot;

    @Test
    void pageDisablesCachingForTheFrontendEntry() throws Exception {
        Files.writeString(webRoot.resolve("index.html"), "<!doctype html><title>Current build</title>");
        PageController controller = new PageController(
                new DefaultResourceLoader(),
                webRoot.toUri().toString()
        );

        ResponseEntity<Resource> response = controller.page();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("no-store, no-cache, must-revalidate", response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
        assertEquals("0", response.getHeaders().getFirst(HttpHeaders.EXPIRES));
    }
}
