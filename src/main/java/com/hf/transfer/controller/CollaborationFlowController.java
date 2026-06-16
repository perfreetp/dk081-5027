package com.hf.transfer.controller;

import com.hf.transfer.common.R;
import com.hf.transfer.common.annotation.OpLog;
import com.hf.transfer.domain.entity.CollaborationTask;
import com.hf.transfer.service.CollaborationFlowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Api(tags = "3. 协同流转模块 - 跨中心任务生成与确认")
@RestController
@RequestMapping("/api/v1/collaboration")
@RequiredArgsConstructor
public class CollaborationFlowController {

    private final CollaborationFlowService collaborationFlowService;

    @ApiOperation("3.1 生成转出确认协同任务")
    @PostMapping("/task/transferOut/{applicationId}")
    @OpLog(logType = "TASK", bizType = "CREATE", module = "协同流转", desc = "生成转出确认任务")
    @ApiImplicitParam(name = "applicationId", value = "申请ID", required = true)
    public R<CollaborationTask> createTransferOutTask(@PathVariable Long applicationId) {
        return R.success("转出任务生成成功", collaborationFlowService.createTransferOutTask(applicationId));
    }

    @ApiOperation("3.2 生成转入确认协同任务")
    @PostMapping("/task/transferIn/{applicationId}")
    @OpLog(logType = "TASK", bizType = "CREATE", module = "协同流转", desc = "生成转入确认任务")
    @ApiImplicitParam(name = "applicationId", value = "申请ID", required = true)
    public R<CollaborationTask> createTransferInTask(@PathVariable Long applicationId) {
        return R.success("转入任务生成成功", collaborationFlowService.createTransferInTask(applicationId));
    }

    @ApiOperation("3.3 转出地确认转出")
    @PostMapping("/task/{taskId}/confirmOut")
    @OpLog(logType = "TASK", bizType = "CONFIRM", module = "协同流转", desc = "转出确认")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "taskId", value = "任务ID", required = true),
            @ApiImplicitParam(name = "actualAmount", value = "实际转出金额"),
            @ApiImplicitParam(name = "remark", value = "处理说明")
    })
    public R<Void> confirmTransferOut(@PathVariable Long taskId,
                                       @RequestParam(required = false) BigDecimal actualAmount,
                                       @RequestParam(required = false) String remark,
                                       @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
                                       @RequestHeader(value = "X-Operator-Name", required = false) String operatorName) {
        collaborationFlowService.confirmTransferOut(taskId, operatorId, operatorName, remark, actualAmount);
        return R.success("转出确认成功", null);
    }

    @ApiOperation("3.4 转入地确认到账")
    @PostMapping("/task/{taskId}/confirmIn")
    @OpLog(logType = "TASK", bizType = "CONFIRM", module = "协同流转", desc = "转入确认")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "taskId", value = "任务ID", required = true),
            @ApiImplicitParam(name = "remark", value = "到账说明")
    })
    public R<Void> confirmTransferIn(@PathVariable Long taskId,
                                      @RequestParam(required = false) String remark,
                                      @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
                                      @RequestHeader(value = "X-Operator-Name", required = false) String operatorName) {
        collaborationFlowService.confirmTransferIn(taskId, operatorId, operatorName, remark);
        return R.success("转入确认成功，申请已办结", null);
    }

    @ApiOperation("3.5 退回任务（退件/要求补正）")
    @PostMapping("/task/{taskId}/reject")
    @OpLog(logType = "TASK", bizType = "REJECT", module = "协同流转", desc = "退回任务")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "taskId", value = "任务ID", required = true),
            @ApiImplicitParam(name = "rejectReasonCode", value = "标准化退件原因编码", required = true),
            @ApiImplicitParam(name = "rejectReasonName", value = "退件原因名称", required = true),
            @ApiImplicitParam(name = "needSupplement", value = "是否需要补正材料：1是 0否", required = true),
            @ApiImplicitParam(name = "supplementItems", value = "需补正材料清单(JSON)"),
            @ApiImplicitParam(name = "remark", value = "退件说明")
    })
    public R<Void> rejectTask(@PathVariable Long taskId,
                               @RequestParam String rejectReasonCode,
                               @RequestParam String rejectReasonName,
                               @RequestParam Boolean needSupplement,
                               @RequestParam(required = false) String supplementItems,
                               @RequestParam(required = false) String remark,
                               @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
                               @RequestHeader(value = "X-Operator-Name", required = false) String operatorName) {
        collaborationFlowService.rejectTask(taskId, operatorId, operatorName,
                rejectReasonCode, rejectReasonName, remark, supplementItems, needSupplement);
        return R.success(needSupplement ? "已退回，等待补正材料" : "已退回，申请流程终止", null);
    }

    @ApiOperation("3.6 查询申请的所有协同任务")
    @GetMapping("/tasks/application/{applicationId}")
    public R<List<CollaborationTask>> getApplicationTasks(@PathVariable Long applicationId) {
        return R.success(collaborationFlowService.getApplicationTasks(applicationId));
    }

    @ApiOperation("3.7 查询任务详情")
    @GetMapping("/task/{taskId}")
    public R<CollaborationTask> getTaskDetail(@PathVariable Long taskId) {
        return R.success(collaborationFlowService.getTaskDetail(taskId));
    }

    @ApiOperation("3.8 手动触发协同任务推送至目标地区")
    @PostMapping("/task/{taskId}/sync")
    @OpLog(logType = "TASK", bizType = "UPDATE", module = "协同流转", desc = "推送任务至目标中心")
    @ApiImplicitParam(name = "taskId", value = "任务ID", required = true)
    public R<Void> syncTaskToTarget(@PathVariable Long taskId) {
        collaborationFlowService.syncTaskToTarget(taskId);
        return R.success("推送成功", null);
    }
}
