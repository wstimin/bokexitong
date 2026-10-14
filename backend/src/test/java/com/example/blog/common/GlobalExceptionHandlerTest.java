package com.example.blog.common;

import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void oversizedUploadsReturnAReadablePayloadTooLargeMessage() {
        Result<Void> result = handler.uploadTooLarge(new MaxUploadSizeExceededException(50L * 1024 * 1024));

        assertEquals(413, result.getCode());
        assertEquals("单个文件不能超过 50MB，请压缩后重试", result.getMessage());
    }

    @Test
    void malformedMultipartRequestsReturnAReadableMessage() {
        Result<Void> result = handler.multipartError(new MultipartException("invalid multipart"));

        assertEquals(400, result.getCode());
        assertEquals("文件上传请求无效，请重新选择文件后重试", result.getMessage());
    }
}
