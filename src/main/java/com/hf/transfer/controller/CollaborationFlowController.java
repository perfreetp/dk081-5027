package com.hf.transfer.controller;

import com.hf.transfer.common.R;
import com.hf.transfer.common.annotation.OpLog;
import com.hf.transfer.domain.entity.CollaborationTask;
import com.hf.transfer.service.CollaborationFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "3. 协同流转模块", description = "跨中心任务生成、转出/转入确认、退件")
@RestController
@RequestMapping("/api/v1/collaboration")
@RequiredArgsConstructor
public class CollaborationFlowController {

    private final CollaborationFlowService collaborationFlowService;

    @Operation(summary = "生成转出确认协同任务", description = "规则校验通过后，自动生成转出地确认任务并推送")
    @PostMapping("/task/transferOut/{applicationId}")
    @OpLog(logType = "TASK", bizType = "CREATE", module = "协同流转", desc = "生成转出确认任务")
    public R<CollaborationTask> createTransferOutTask(
            @Parameter(description = "申请ID") @PathVariable Long applicationId) {
        return R.success("转出任务生成成功", collaborationFlowService.createTransferOutTask(applicationId));
    }

    @Operation(summary = "生成转入确认协同任务", description = "转出确认后，自动生成转入地确认任务")
    @PostMapping("/task/transferIn/{applicationId}")
    @OpLog(logType = "TASK", bizType = "CREATE", module = "协同流转", desc = "生成转入确认任务")
    public R<CollaborationTask> createTransferInTask(
            @Parameter(description = "申请ID") @PathVariable Long applicationId) {
        return R.success("转入任务生成成功", collaborationFlowService.createTransferInTask(applicationId));
    }

    @Operation(summary = "转出地确认转出", description = "转出地确认后自动生成转入任务")
    @PostMapping("/task/{taskId}/confirmOut")
    @OpLog(logType = "TASK", bizType = "CONFIRM", module = "协同流转", desc = "转出确认")
    public R<Void> confirmTransferOut(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @Parameter(description = "实际转出金额") @RequestParam(required = false) BigDecimal actualAmount,
            @Parameter(description = "处理说明") @RequestParam(required = false) String remark,
            @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
            @RequestHeader(value = "X-Operator-Name", required = false) String operatorName) {
        collaborationFlowService.confirmTransferOut(taskId, operatorId, operatorName, remark, actualAmount);
        return R.success("转出确认成功", null);
    }

    @Operation(summary = "转入地确认到账", description = "转入确认后申请自动办结归档")
    @PostMapping("/task/{taskId}/confirmIn")
    @OpLog(logType = "TASK", bizType = "CONFIRM", module = "协同流转", desc = "转入确认")
    public R<Void> confirmTransferIn(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @Parameter(description = "到账说明") @RequestParam(required = false) String remark,
            @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
            @RequestHeader(value = "X-Operator-Name", required = false) String operatorName) {
        collaborationFlowService.confirmTransferIn(taskId, operatorId, operatorName, remark);
        return R.success("转入确认成功，申请已办结", null);
    }

    @Operation(summary = "退回任务（退件/要求补正）", description = "使用标准化退件原因，选择是否允许补正")
    @PostMapping("/task/{taskId}/reject")
    @OpLog(logType = "TASK", bizType = "REJECT", module = "协同流转", desc = "退回任务")
    public R<Void> rejectTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @Parameter(description = "标准化退件原因编码") @RequestParam String rejectReasonCode,
            @Parameter(description = "退件原因名称") @RequestParam String rejectReasonName,
            @Parameter(description = "是否需要补正材料") @RequestParam Boolean needSupplement,
            @Parameter(description = "需补正材料清单(JSON)") @RequestParam(required = false) String supplementItems,
            @Parameter(description = "退件说明") @RequestParam(required = false) String remark,
            @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
            @RequestHeader(value = "X-Operator-Name", required = false) String operatorName) {
        collaborationFlowService.rejectTask(taskId, operatorId, operatorName,
                rejectReasonCode, rejectReasonName, remark, supplementItems, needSupplement);
        return R.success(needSupplement ? "已退回，等待补正材料" : "已退回，申请流程终止", null);
    }

    @Operation(summary = "查询申请的所有协同任务")
    @GetMapping("/tasks/application/{applicationId}")
    public R<List<CollaborationTask>> getApplicationTasks(
            @Parameter(description = "申请ID") @PathVariable Long applicationId) {
        return R.success(collaborationFlowService.getApplicationTasks(applicationId));
    }

    @Operation(summary = "查询任务详情")
    @GetMapping("/task/{taskId}")
    public R<CollaborationTask> getTaskDetail(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        return R.success(collaborationFlowService.getTaskDetail(taskId));
    }

    @Operation(summary = "手动推送协同任务至目标地区")
    @PostMapping("/task/{taskId}/sync")
    @OpLog(logType = "TASK", bizType = "UPDATE", module = "协同流转", desc = "推送任务至目标中心")
    public R<Void> syncTaskToTarget(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        collaborationFlowService.syncTaskToTarget(taskId);
        return R.success("推送成功", null);
    }
}
