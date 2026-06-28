package com.example.blog.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String nickname;
    private String password;
    private String email;
}
