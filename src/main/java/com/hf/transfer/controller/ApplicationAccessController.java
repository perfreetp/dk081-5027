package com.hf.transfer.controller;

import com.hf.transfer.common.R;
import com.hf.transfer.common.PageResult;
import com.hf.transfer.common.annotation.OpLog;
import com.hf.transfer.domain.dto.ApplicationQueryDTO;
import com.hf.transfer.domain.dto.TransferApplyDTO;
import com.hf.transfer.domain.vo.ApplicationDetailVO;
import com.hf.transfer.domain.vo.ApplicationListVO;
import com.hf.transfer.domain.vo.ProgressQueryVO;
import com.hf.transfer.service.ApplicationAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Tag(name = "1. 申请接入模块", description = "统一接收各渠道转移申请、进度查询")
@RestController
@RequestMapping("/api/v1/application")
@RequiredArgsConstructor
@Validated
public class ApplicationAccessController {

    private final ApplicationAccessService applicationAccessService;

    @Operation(summary = "提交转移申请", description = "统一接收各渠道（网厅/APP/小程序/柜台/API等）提交的异地转移接续申请")
    @PostMapping("/submit")
    @OpLog(logType = "APPLICATION", bizType = "CREATE", module = "申请接入", desc = "提交异地转移接续申请")
    public R<String> submitApplication(@RequestBody @Valid TransferApplyDTO dto,
                                       @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
                                       @RequestHeader(value = "X-Operator-Name", required = false) String operatorName) {
        String applicationNo = applicationAccessService.submitApplication(dto, operatorId, operatorName);
        return R.success("申请提交成功", applicationNo);
    }

    @Operation(summary = "对外进度查询", description = "按申请编号+证件号查询办理进度，返回5步进度条")
    @GetMapping("/progress")
    public R<ProgressQueryVO> queryProgress(
            @Parameter(description = "申请编号") @RequestParam String applicationNo,
            @Parameter(description = "证件号码") @RequestParam(required = false) String idCardNo) {
        return R.success(applicationAccessService.queryProgress(applicationNo, idCardNo));
    }

    @Operation(summary = "申请详情查询", description = "内部使用，查看完整信息含材料/状态日志/协同任务")
    @GetMapping("/{id}")
    @OpLog(logType = "APPLICATION", bizType = "QUERY", module = "申请接入", desc = "查询申请详情")
    public R<ApplicationDetailVO> getDetail(
            @Parameter(description = "申请ID") @PathVariable Long id) {
        return R.success(applicationAccessService.getApplicationDetail(id));
    }

    @Operation(summary = "申请列表分页查询", description = "支持按地区/时间/状态/类型/渠道等多维度筛选")
    @PostMapping("/page")
    @OpLog(logType = "APPLICATION", bizType = "QUERY", module = "申请接入", desc = "分页查询申请列表")
    public R<PageResult<ApplicationListVO>> queryPage(@RequestBody ApplicationQueryDTO dto) {
        return R.success(applicationAccessService.queryApplicationPage(dto));
    }

    @Operation(summary = "取消申请", description = "取消处于待审核/规则通过/转出待受理/待补正状态的申请")
    @PostMapping("/{id}/cancel")
    @OpLog(logType = "APPLICATION", bizType = "UPDATE", module = "申请接入", desc = "取消转移申请")
    public R<Void> cancelApplication(
            @Parameter(description = "申请ID") @PathVariable Long id,
            @Parameter(description = "取消原因") @RequestParam String reason,
            @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
            @RequestHeader(value = "X-Operator-Name", required = false) String operatorName) {
        applicationAccessService.cancelApplication(id, operatorId, operatorName, reason);
        return R.success("取消成功", null);
    }
}
