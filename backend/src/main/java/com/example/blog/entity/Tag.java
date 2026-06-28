package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tag")
public class Tag {
    private Long id;
    private String name;
    private String color;
    private LocalDateTime createdAt;
}
