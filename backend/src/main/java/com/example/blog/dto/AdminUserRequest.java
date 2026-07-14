package com.example.blog.dto;

import lombok.Data;

@Data
public class AdminUserRequest {
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String password;
    private String role;
    private Integer status;
}
