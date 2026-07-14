package com.example.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.entity.OperationLog;
import com.example.blog.mapper.OperationLogMapper;
import com.example.blog.security.BlogPrincipal;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OperationLogService {
    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public Page<OperationLog> page(long current, long size, String action, String targetType, String keyword) {
        return operationLogMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<OperationLog>()
                .eq(action != null && !action.isBlank(), OperationLog::getAction, action)
                .eq(targetType != null && !targetType.isBlank(), OperationLog::getTargetType, targetType)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                        .like(OperationLog::getOperatorName, keyword)
                        .or().like(OperationLog::getDetail, keyword))
                .orderByDesc(OperationLog::getCreatedAt));
    }

    public void record(BlogPrincipal principal, String action, String targetType, Long targetId, String detail) {
        OperationLog log = new OperationLog();
        if (principal != null) {
            log.setOperatorId(principal.userId());
            log.setOperatorName(principal.username());
        }
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}
