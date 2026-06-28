package com.example.blog.dto;

import com.example.blog.entity.BlogUser;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private BlogUser user;
}
