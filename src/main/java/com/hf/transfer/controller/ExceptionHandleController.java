package com.hf.transfer.controller;

import com.hf.transfer.common.PageResult;
import com.hf.transfer.common.R;
import com.hf.transfer.common.annotation.OpLog;
import com.hf.transfer.domain.dto.SupplementSubmitDTO;
import com.hf.transfer.domain.entity.RejectReason;
import com.hf.transfer.domain.entity.UrgeRecord;
import com.hf.transfer.domain.vo.SupplementDetailVO;
import com.hf.transfer.service.ExceptionHandleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Api(tags = "4. 异常处置模块 - 超时催办、退件标准化、补正流转")
@RestController
@RequestMapping("/api/v1/exception")
@RequiredArgsConstructor
public class ExceptionHandleController {

    private final ExceptionHandleService exceptionHandleService;

    @ApiOperation("4.1 手动触发超时催办扫描")
    @PostMapping("/timeout/scan")
    @OpLog(logType = "SYSTEM", bizType = "URGE", module = "异常处置", desc = "执行超时催办扫描")
    public R<Void> processTimeoutTasks() {
        exceptionHandleService.processTimeoutTasks();
        return R.success("催办扫描执行完成", null);
    }

    @ApiOperation("4.2 人工催办指定任务")
    @PostMapping("/urge/task/{taskId}")
    @OpLog(logType = "URGE", bizType = "URGE", module = "异常处置", desc = "人工催办任务")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "taskId", value = "任务ID", required = true),
            @ApiImplicitParam(name = "urgeLevel", value = "催办级别：1一般 2重要 3加急"),
            @ApiImplicitParam(name = "content", value = "催办内容")
    })
    public R<UrgeRecord> urgeTaskManually(@PathVariable Long taskId,
                                           @RequestParam(defaultValue = "2") Integer urgeLevel,
                                           @RequestParam(required = false) String content,
                                           @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
                                           @RequestHeader(value = "X-Operator-Name", required = false) String operatorName) {
        return R.success("催办成功",
                exceptionHandleService.urgeTaskManually(taskId, urgeLevel, content, operatorId, operatorName));
    }

    @ApiOperation("4.3 查询标准化退件原因列表")
    @GetMapping("/reject-reasons")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "category", value = "原因分类：1材料类 2信息类 3规则类 4账户类 5其他"),
            @ApiImplicitParam(name = "scene", value = "适用环节：0全部 1规则校验 2转出审核 3转入审核")
    })
    public R<List<RejectReason>> listRejectReasons(@RequestParam(required = false) Integer category,
                                                     @RequestParam(required = false) Integer scene) {
        return R.success(exceptionHandleService.listRejectReasons(category, scene));
    }

    @ApiOperation("4.4 发起补正材料要求")
    @PostMapping("/supplement/request")
    @OpLog(logType = "SUPPLEMENT", bizType = "SUPPLEMENT", module = "异常处置", desc = "发起补正材料要求")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "applicationId", value = "申请ID", required = true),
            @ApiImplicitParam(name = "taskId", value = "关联任务ID"),
            @ApiImplicitParam(name = "requestRegion", value = "发起地区编码", required = true),
            @ApiImplicitParam(name = "requestRemark", value = "补正要求说明", required = true),
            @ApiImplicitParam(name = "requiredItems", value = "需补正材料清单(JSON)"),
            @ApiImplicitParam(name = "deadlineDays", value = "补正截止天数")
    })
    public R<Void> requestSupplement(@RequestParam Long applicationId,
                                      @RequestParam(required = false) Long taskId,
                                      @RequestParam String requestRegion,
                                      @RequestParam String requestRemark,
                                      @RequestParam(required = false) String requiredItems,
                                      @RequestParam(defaultValue = "7") Integer deadlineDays,
                                      @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
                                      @RequestHeader(value = "X-Operator-Name", required = false) String operatorName) {
        exceptionHandleService.requestSupplement(applicationId, taskId, requestRegion,
                operatorId, operatorName, requestRemark, requiredItems, deadlineDays);
        return R.success("补正要求已发出", null);
    }

    @ApiOperation("4.5 查询补正详情（按补正ID）")
    @GetMapping("/supplement/{supplementId}")
    public R<SupplementDetailVO> getSupplementDetail(@PathVariable Long supplementId) {
        return R.success(exceptionHandleService.getSupplementDetail(supplementId));
    }

    @ApiOperation("4.6 查询申请的最新补正详情")
    @GetMapping("/supplement/application/{applicationId}")
    public R<SupplementDetailVO> getSupplementByApplication(@PathVariable Long applicationId) {
        return R.success(exceptionHandleService.getSupplementByApplication(applicationId));
    }

    @ApiOperation("4.7 提交补正材料")
    @PostMapping("/supplement/submit")
    @OpLog(logType = "SUPPLEMENT", bizType = "SUPPLEMENT", module = "异常处置", desc = "提交补正材料")
    public R<Long> submitSupplementMaterials(@RequestBody SupplementSubmitDTO dto) {
        Long id = exceptionHandleService.submitSupplementMaterials(
                dto.getApplicationId(),
                dto.getSupplementId(),
                dto.getSubmitOperatorId(),
                dto.getSubmitOperatorName(),
                dto.getMaterials());
        return R.success("补正材料提交成功", id);
    }

    @ApiOperation("4.8 审核补正材料")
    @PostMapping("/supplement/{supplementId}/audit")
    @OpLog(logType = "SUPPLEMENT", bizType = "AUDIT", module = "异常处置", desc = "审核补正材料")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "supplementId", value = "补正ID", required = true),
            @ApiImplicitParam(name = "auditResult", value = "审核结果：1通过 2不通过", required = true),
            @ApiImplicitParam(name = "auditRemark", value = "审核意见")
    })
    public R<Void> auditSupplement(@PathVariable Long supplementId,
                                    @RequestParam Integer auditResult,
                                    @RequestParam(required = false) String auditRemark,
                                    @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
                                    @RequestHeader(value = "X-Operator-Name", required = false) String operatorName) {
        exceptionHandleService.auditSupplement(supplementId, auditResult, auditRemark, operatorId, operatorName);
        return R.success(auditResult == 1 ? "补正审核通过，已恢复流转" : "补正审核不通过", null);
    }

    @ApiOperation("4.9 分页查询催办记录")
    @GetMapping("/urge-records/page")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "current", value = "页码"),
            @ApiImplicitParam(name = "size", value = "每页数量"),
            @ApiImplicitParam(name = "taskId", value = "任务ID"),
            @ApiImplicitParam(name = "applicationNo", value = "申请编号"),
            @ApiImplicitParam(name = "targetRegion", value = "被催办地区"),
            @ApiImplicitParam(name = "urgeType", value = "催办类型：1系统自动 2人工")
    })
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
