package com.hf.transfer.controller;

import com.hf.transfer.common.PageResult;
import com.hf.transfer.common.R;
import com.hf.transfer.domain.entity.OperationLog;
import com.hf.transfer.service.OperationLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Api(tags = "6. 操作审计 - 全过程操作留痕查询")
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final OperationLogService operationLogService;

    @ApiOperation("6.1 分页查询操作审计日志")
    @GetMapping("/log/page")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "logType", value = "日志类型：APPLICATION申请 TASK任务 RULE规则 QUERY查询 SYSTEM系统"),
            @ApiImplicitParam(name = "bizType", value = "业务类型：CREATE创建 UPDATE修改 AUDIT审核 CONFIRM确认 REJECT退回 URGE催办 SUPPLEMENT补正 EXPORT导出"),
            @ApiImplicitParam(name = "operatorId", value = "操作人ID"),
            @ApiImplicitParam(name = "bizNo", value = "业务编号(模糊)"),
            @ApiImplicitParam(name = "executeResult", value = "执行结果：1成功 0失败")
    })
    public R<PageResult<OperationLog>> queryLogPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String logType,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String operatorId,
            @RequestParam(required = false) String bizNo,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) Integer executeResult) {
        return R.success(operationLogService.queryLogPage(
                current, size, logType, bizType, operatorId, bizNo, startTime, endTime, executeResult));
    }
}
