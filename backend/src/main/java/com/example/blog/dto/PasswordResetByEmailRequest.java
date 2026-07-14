package com.example.blog.dto;

import lombok.Data;

@Data
public class PasswordResetByEmailRequest {
    private String email;
    private String code;
    private String newPassword;
}
