package com.hf.transfer.controller;

import com.hf.transfer.common.PageResult;
import com.hf.transfer.common.R;
import com.hf.transfer.common.annotation.OpLog;
import com.hf.transfer.domain.dto.SupplementSubmitDTO;
import com.hf.transfer.domain.entity.RejectReason;
import com.hf.transfer.domain.entity.UrgeRecord;
import com.hf.transfer.domain.vo.SupplementDetailVO;
import com.hf.transfer.service.ExceptionHandleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "4. 异常处置模块", description = "超时催办、退件标准化、补正材料再流转")
@RestController
@RequestMapping("/api/v1/exception")
@RequiredArgsConstructor
public class ExceptionHandleController {

    private final ExceptionHandleService exceptionHandleService;

    @Operation(summary = "手动触发超时催办扫描", description = "扫描所有超时未处理的协同任务并自动催办")
    @PostMapping("/timeout/scan")
    @OpLog(logType = "SYSTEM", bizType = "URGE", module = "异常处置", desc = "执行超时催办扫描")
    public R<Void> processTimeoutTasks() {
        exceptionHandleService.processTimeoutTasks();
        return R.success("催办扫描执行完成", null);
    }

    @Operation(summary = "人工催办指定任务", description = "支持3级催办：1一般提醒 2重要提醒 3加急")
    @PostMapping("/urge/task/{taskId}")
    @OpLog(logType = "URGE", bizType = "URGE", module = "异常处置", desc = "人工催办任务")
    public R<UrgeRecord> urgeTaskManually(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @Parameter(description = "催办级别：1一般 2重要 3加急") @RequestParam(defaultValue = "2") Integer urgeLevel,
            @Parameter(description = "催办内容") @RequestParam(required = false) String content,
            @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
            @RequestHeader(value = "X-Operator-Name", required = false) String operatorName) {
        return R.success("催办成功",
                exceptionHandleService.urgeTaskManually(taskId, urgeLevel, content, operatorId, operatorName));
    }

    @Operation(summary = "查询标准化退件原因列表", description = "15条预置退件原因，按类型+适用环节筛选")
    @GetMapping("/reject-reasons")
    public R<List<RejectReason>> listRejectReasons(
            @Parameter(description = "原因分类：1材料类 2信息类 3规则类 4账户类 5其他") @RequestParam(required = false) Integer category,
            @Parameter(description = "适用环节：0全部 1规则校验 2转出审核 3转入审核") @RequestParam(required = false) Integer scene) {
        return R.success(exceptionHandleService.listRejectReasons(category, scene));
    }

    @Operation(summary = "发起补正材料要求", description = "向申请人发出补正材料通知，设定截止日期")
    @PostMapping("/supplement/request")
    @OpLog(logType = "SUPPLEMENT", bizType = "SUPPLEMENT", module = "异常处置", desc = "发起补正材料要求")
    public R<Void> requestSupplement(
            @Parameter(description = "申请ID") @RequestParam Long applicationId,
            @Parameter(description = "关联任务ID") @RequestParam(required = false) Long taskId,
            @Parameter(description = "发起地区编码") @RequestParam String requestRegion,
            @Parameter(description = "补正要求说明") @RequestParam String requestRemark,
            @Parameter(description = "需补正材料清单(JSON)") @RequestParam(required = false) String requiredItems,
            @Parameter(description = "补正截止天数") @RequestParam(defaultValue = "7") Integer deadlineDays,
            @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
            @RequestHeader(value = "X-Operator-Name", required = false) String operatorName) {
        exceptionHandleService.requestSupplement(applicationId, taskId, requestRegion,
                operatorId, operatorName, requestRemark, requiredItems, deadlineDays);
        return R.success("补正要求已发出", null);
    }

    @Operation(summary = "查询补正详情（按补正ID）")
    @GetMapping("/supplement/{supplementId}")
    public R<SupplementDetailVO> getSupplementDetail(
            @Parameter(description = "补正ID") @PathVariable Long supplementId) {
        return R.success(exceptionHandleService.getSupplementDetail(supplementId));
    }

    @Operation(summary = "查询申请的最新补正详情")
    @GetMapping("/supplement/application/{applicationId}")
    public R<SupplementDetailVO> getSupplementByApplication(
            @Parameter(description = "申请ID") @PathVariable Long applicationId) {
        return R.success(exceptionHandleService.getSupplementByApplication(applicationId));
    }

    @Operation(summary = "提交补正材料", description = "申请人补充材料后重新进入审核流程")
    @PostMapping("/supplement/submit")
    @OpLog(logType = "SUPPLEMENT", bizType = "SUPPLEMENT", module = "异常处置", desc = "提交补正材料")
    public R<Long> submitSupplementMaterials(@RequestBody SupplementSubmitDTO dto) {
        Long id = exceptionHandleService.submitSupplementMaterials(
                dto.getApplicationId(), dto.getSupplementId(),
                dto.getSubmitOperatorId(), dto.getSubmitOperatorName(), dto.getMaterials());
        return R.success("补正材料提交成功", id);
    }

    @Operation(summary = "审核补正材料", description = "通过则恢复流转，不通过则继续补正")
    @PostMapping("/supplement/{supplementId}/audit")
    @OpLog(logType = "SUPPLEMENT", bizType = "AUDIT", module = "异常处置", desc = "审核补正材料")
    public R<Void> auditSupplement(
            @Parameter(description = "补正ID") @PathVariable Long supplementId,
            @Parameter(description = "审核结果：1通过 2不通过") @RequestParam Integer auditResult,
            @Parameter(description = "审核意见") @RequestParam(required = false) String auditRemark,
            @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
            @RequestHeader(value = "X-Operator-Name", required = false) String operatorName) {
        exceptionHandleService.auditSupplement(supplementId, auditResult, auditRemark, operatorId, operatorName);
        return R.success(auditResult == 1 ? "补正审核通过，已恢复流转" : "补正审核不通过", null);
    }

    @Operation(summary = "分页查询催办记录")
    @GetMapping("/urge-records/page")
    public R<PageResult<UrgeRecord>> queryUrgeRecords(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String applicationNo,
            @RequestParam(required = false) String targetRegion,
            @RequestParam(required = false) Integer urgeType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.success(exceptionHandleService.queryUrgeRecords(
                current, size, taskId, applicationNo, targetRegion, urgeType, startTime, endTime));
    }
}
