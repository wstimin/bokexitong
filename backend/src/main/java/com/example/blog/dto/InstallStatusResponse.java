package com.example.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InstallStatusResponse {
    private boolean installed;
    private String siteName;
    private String domain;
}
