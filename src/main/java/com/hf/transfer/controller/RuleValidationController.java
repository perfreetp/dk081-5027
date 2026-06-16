package com.hf.transfer.controller;

import com.hf.transfer.common.R;
import com.hf.transfer.common.annotation.OpLog;
import com.hf.transfer.domain.dto.TransferApplyDTO;
import com.hf.transfer.domain.entity.RegionRule;
import com.hf.transfer.domain.entity.TransferApplication;
import com.hf.transfer.service.RuleValidationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "2. 规则校验模块 - 地区规则匹配、重复冲突识别")
@RestController
@RequestMapping("/api/v1/rule")
@RequiredArgsConstructor
public class RuleValidationController {

    private final RuleValidationService ruleValidationService;

    @ApiOperation("2.1 匹配地区受理规则")
    @GetMapping("/match")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "regionCode", value = "地区编码", required = true),
            @ApiImplicitParam(name = "ruleType", value = "规则类型：1转出 2转入 3通用", required = true)
    })
    public R<RegionRule> matchRegionRule(@RequestParam String regionCode,
                                          @RequestParam Integer ruleType) {
        return R.success(ruleValidationService.matchRegionRule(regionCode, ruleType));
    }

    @ApiOperation("2.2 为申请匹配转出/转入规则")
    @PostMapping("/matchForApplication")
    public R<List<RegionRule>> matchForApplication(@RequestBody TransferApplyDTO dto) {
        return R.success(ruleValidationService.matchRulesForApplication(dto));
    }

    @ApiOperation("2.3 基础规则校验")
    @PostMapping("/validate")
    @OpLog(logType = "RULE", bizType = "AUDIT", module = "规则校验", desc = "执行基础规则校验")
    public R<RuleValidationService.ValidationResult> validate(@RequestBody TransferApplyDTO dto) {
        return R.success(ruleValidationService.validateApplication(dto));
    }

    @ApiOperation("2.4 重复申请检测")
    @PostMapping("/checkDuplicate")
    public R<RuleValidationService.DuplicateCheckResult> checkDuplicate(@RequestBody TransferApplyDTO dto) {
        return R.success(ruleValidationService.checkDuplicate(dto));
    }

    @ApiOperation("2.5 冲突申请检测")
    @PostMapping("/checkConflict")
    public R<RuleValidationService.ConflictCheckResult> checkConflict(@RequestBody TransferApplyDTO dto) {
        return R.success(ruleValidationService.checkConflict(dto));
    }

    @ApiOperation("2.6 完整规则校验（含重复/冲突/规则）")
    @PostMapping("/validateFull")
    @OpLog(logType = "RULE", bizType = "AUDIT", module = "规则校验", desc = "执行完整规则校验")
    @ApiImplicitParam(name = "applicationId", value = "申请ID", required = true)
    public R<RuleValidationService.RuleValidateVO> validateFull(@RequestParam Long applicationId) {
        TransferApplication app = new TransferApplication();
        app.setId(applicationId);
        return R.success(ruleValidationService.validateFull(app));
    }
}
