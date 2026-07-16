package com.example.blog.dto;

import lombok.Data;

@Data
public class InstallRequest {
    private String siteName;
    private String domain;
    private String adminPassword;
    private String dbHost;
    private String dbPort;
    private String dbName;
    private String dbUsername;
    private String dbPassword;
    private String mysqlRootPassword;
    private String jwtSecret;
}
