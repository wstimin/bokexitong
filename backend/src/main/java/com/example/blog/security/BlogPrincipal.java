package com.example.blog.security;

public record BlogPrincipal(Long userId, String username, String role) {
}
