package com.example.blog.dto;

import lombok.Data;

@Data
public class ProfileRequest {
    private String nickname;
    private String avatar;
    private String email;
}
