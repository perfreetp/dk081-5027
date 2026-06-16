package com.hf.transfer.controller;

import com.hf.transfer.common.PageResult;
import com.hf.transfer.common.R;
import com.hf.transfer.domain.entity.OperationLog;
import com.hf.transfer.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "6. 操作审计", description = "全过程操作留痕查询")
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final OperationLogService operationLogService;

    @Operation(summary = "分页查询操作审计日志", description = "按日志类型/业务类型/操作人/执行结果等条件查询")
    @GetMapping("/log/page")
    public R<PageResult<OperationLog>> queryLogPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "日志类型") @RequestParam(required = false) String logType,
            @Parameter(description = "业务类型") @RequestParam(required = false) String bizType,
            @Parameter(description = "操作人ID") @RequestParam(required = false) String operatorId,
            @Parameter(description = "业务编号") @RequestParam(required = false) String bizNo,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "执行结果：1成功 0失败") @RequestParam(required = false) Integer executeResult) {
        return R.success(operationLogService.queryLogPage(
                current, size, logType, bizType, operatorId, bizNo, startTime, endTime, executeResult));
    }
}
