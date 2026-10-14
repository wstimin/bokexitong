package com.example.blog.common;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Result<Void> uploadTooLarge(MaxUploadSizeExceededException ex) {
        return Result.fail(HttpStatus.PAYLOAD_TOO_LARGE.value(), "单个文件不能超过 50MB，请压缩后重试");
    }

    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> multipartError(MultipartException ex) {
        return Result.fail(HttpStatus.BAD_REQUEST.value(), "文件上传请求无效，请重新选择文件后重试");
    }

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
