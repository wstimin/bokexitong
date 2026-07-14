package com.example.blog.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String account;
    private String username;
    private String password;
    private String loginType;
}
