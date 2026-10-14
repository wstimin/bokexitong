package com.example.blog.controller;

import com.example.blog.common.Result;
import com.example.blog.security.BlogPrincipal;
import com.example.blog.service.OperationLogService;
import com.example.blog.service.UpdateService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/update")
public class UpdateController {
    private final UpdateService updateService;
    private final OperationLogService operationLogService;

    public UpdateController(UpdateService updateService, OperationLogService operationLogService) {
        this.updateService = updateService;
        this.operationLogService = operationLogService;
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        return Result.ok(updateService.status());
    }

    @PostMapping("/check")
    public Result<Map<String, Object>> check() {
        return Result.ok(updateService.check());
    }

    @PostMapping("/install")
    public Result<Map<String, Object>> install(@AuthenticationPrincipal BlogPrincipal principal) {
        Map<String, Object> task = updateService.requestUpdate();
        operationLogService.record(principal, "UPDATE", "SYSTEM", null,
                "提交版本更新：" + task.get("fromVersion") + " -> " + task.get("toVersion"));
        return Result.ok(task);
    }
}
