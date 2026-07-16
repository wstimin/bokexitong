package com.example.blog.dto;

import lombok.Data;

@Data
public class InstallRequest {
    private String siteName;
    private String domain;
    private String adminPassword;
    private String mysqlRootPassword;
    private String jwtSecret;
}
