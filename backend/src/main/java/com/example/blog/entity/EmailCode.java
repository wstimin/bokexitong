package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("email_code")
public class EmailCode {
    private Long id;
    private String email;
    private String codeHash;
    private String scene;
    private Integer used;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime usedAt;
}
