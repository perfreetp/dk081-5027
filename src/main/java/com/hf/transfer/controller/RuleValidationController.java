package com.hf.transfer.controller;

import com.hf.transfer.common.R;
import com.hf.transfer.common.annotation.OpLog;
import com.hf.transfer.domain.dto.TransferApplyDTO;
import com.hf.transfer.domain.entity.RegionRule;
import com.hf.transfer.domain.entity.TransferApplication;
import com.hf.transfer.mapper.TransferApplicationMapper;
import com.hf.transfer.service.RuleValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "2. 规则校验模块", description = "地区规则匹配、重复/冲突申请识别")
@RestController
@RequestMapping("/api/v1/rule")
@RequiredArgsConstructor
public class RuleValidationController {

    private final RuleValidationService ruleValidationService;
    private final TransferApplicationMapper applicationMapper;

    @Operation(summary = "匹配地区受理规则", description = "按地区编码+规则类型匹配当前有效的受理规则")
    @GetMapping("/match")
    public R<RegionRule> matchRegionRule(
            @Parameter(description = "地区编码") @RequestParam String regionCode,
            @Parameter(description = "规则类型：1转出 2转入 3通用") @RequestParam Integer ruleType) {
        return R.success(ruleValidationService.matchRegionRule(regionCode, ruleType));
    }

    @Operation(summary = "为申请匹配转出/转入规则", description = "同时匹配转出地+转入地规则")
    @PostMapping("/matchForApplication")
    public R<List<RegionRule>> matchForApplication(@RequestBody TransferApplyDTO dto) {
        return R.success(ruleValidationService.matchRulesForApplication(dto));
    }

    @Operation(summary = "基础规则校验", description = "校验金额/限额/属地化等基础规则")
    @PostMapping("/validate")
    @OpLog(logType = "RULE", bizType = "AUDIT", module = "规则校验", desc = "执行基础规则校验")
    public R<RuleValidationService.ValidationResult> validate(@RequestBody TransferApplyDTO dto) {
        return R.success(ruleValidationService.validateApplication(dto));
    }

    @Operation(summary = "重复申请检测", description = "检测90天内同证件号+同转出转入地的在途申请")
    @PostMapping("/checkDuplicate")
    public R<RuleValidationService.DuplicateCheckResult> checkDuplicate(@RequestBody TransferApplyDTO dto) {
        return R.success(ruleValidationService.checkDuplicate(dto));
    }

    @Operation(summary = "冲突申请检测", description = "检测同转出地向不同地区转出、或同地区多笔转入")
    @PostMapping("/checkConflict")
    public R<RuleValidationService.ConflictCheckResult> checkConflict(@RequestBody TransferApplyDTO dto) {
        return R.success(ruleValidationService.checkConflict(dto));
    }

    @Operation(summary = "完整规则校验", description = "含重复/冲突/规则全量校验，返回通过项和未通过项")
    @PostMapping("/validateFull")
    @OpLog(logType = "RULE", bizType = "AUDIT", module = "规则校验", desc = "执行完整规则校验")
    public R<RuleValidationService.RuleValidateVO> validateFull(
            @Parameter(description = "申请ID") @RequestParam Long applicationId) {
        TransferApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            return R.error("申请不存在");
        }
        return R.success(ruleValidationService.validateFull(app));
    }
}
