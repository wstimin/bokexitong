package com.example.blog.common;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> badRequest(IllegalArgumentException ex) {
        return Result.fail(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> forbidden(AccessDeniedException ex) {
        return Result.fail(HttpStatus.FORBIDDEN.value(), "没有访问权限");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> error(Exception ex) {
        return Result.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
    }
}
