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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Api(tags = "1. 申请接入模块 - 统一接收各渠道转移申请")
@RestController
@RequestMapping("/api/v1/application")
@RequiredArgsConstructor
@Validated
public class ApplicationAccessController {

    private final ApplicationAccessService applicationAccessService;

    @ApiOperation("1.1 提交转移申请 - 统一接收各渠道申请")
    @PostMapping("/submit")
    @OpLog(logType = "APPLICATION", bizType = "CREATE", module = "申请接入", desc = "提交异地转移接续申请")
    public R<String> submitApplication(@RequestBody @Valid TransferApplyDTO dto,
                                       @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
                                       @RequestHeader(value = "X-Operator-Name", required = false) String operatorName) {
        String applicationNo = applicationAccessService.submitApplication(dto, operatorId, operatorName);
        return R.success("申请提交成功", applicationNo);
    }

    @ApiOperation("1.2 对外进度查询 - 按申请编号+证件号查询办理进度")
    @GetMapping("/progress")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "applicationNo", value = "申请编号", required = true),
            @ApiImplicitParam(name = "idCardNo", value = "证件号码(脱敏后比对)", required = false)
    })
    public R<ProgressQueryVO> queryProgress(@RequestParam String applicationNo,
                                             @RequestParam(required = false) String idCardNo) {
        return R.success(applicationAccessService.queryProgress(applicationNo, idCardNo));
    }

    @ApiOperation("1.3 申请详情查询 - 内部使用，查看完整信息")
    @GetMapping("/{id}")
    @OpLog(logType = "APPLICATION", bizType = "QUERY", module = "申请接入", desc = "查询申请详情")
    public R<ApplicationDetailVO> getDetail(@PathVariable Long id) {
        return R.success(applicationAccessService.getApplicationDetail(id));
    }

    @ApiOperation("1.4 申请列表分页查询")
    @PostMapping("/page")
    @OpLog(logType = "APPLICATION", bizType = "QUERY", module = "申请接入", desc = "分页查询申请列表")
    public R<PageResult<ApplicationListVO>> queryPage(@RequestBody ApplicationQueryDTO dto) {
        return R.success(applicationAccessService.queryApplicationPage(dto));
    }

    @ApiOperation("1.5 取消申请")
    @PostMapping("/{id}/cancel")
    @OpLog(logType = "APPLICATION", bizType = "UPDATE", module = "申请接入", desc = "取消转移申请")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "申请ID", required = true),
            @ApiImplicitParam(name = "reason", value = "取消原因", required = true)
    })
    public R<Void> cancelApplication(@PathVariable Long id,
                                      @RequestParam String reason,
                                      @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
                                      @RequestHeader(value = "X-Operator-Name", required = false) String operatorName) {
        applicationAccessService.cancelApplication(id, operatorId, operatorName, reason);
        return R.success("取消成功", null);
    }
}
