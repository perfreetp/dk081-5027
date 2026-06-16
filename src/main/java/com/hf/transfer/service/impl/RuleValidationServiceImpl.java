package com.hf.transfer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hf.transfer.common.BusinessErrorEnum;
import com.hf.transfer.common.BusinessException;
import com.hf.transfer.domain.dto.TransferApplyDTO;
import com.hf.transfer.domain.entity.RegionRule;
import com.hf.transfer.domain.entity.TransferApplication;
import com.hf.transfer.common.enums.ApplicationStatusEnum;
import com.hf.transfer.mapper.RegionRuleMapper;
import com.hf.transfer.mapper.TransferApplicationMapper;
import com.hf.transfer.service.RuleValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleValidationServiceImpl implements RuleValidationService {

    private final RegionRuleMapper regionRuleMapper;
    private final TransferApplicationMapper applicationMapper;

    private static final List<Integer> PROCESSING_STATUS = Arrays.asList(
            10, 20, 30, 40, 50, 70
    );

    @Override
    public RegionRule matchRegionRule(String regionCode, Integer ruleType) {
        LambdaQueryWrapper<RegionRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RegionRule::getRegionCode, regionCode)
                .eq(RegionRule::getRuleType, ruleType)
                .eq(RegionRule::getStatus, 1)
                .le(RegionRule::getEffectiveDate, LocalDateTime.now())
                .and(w -> w.isNull(RegionRule::getExpiryDate)
                        .or()
                        .ge(RegionRule::getExpiryDate, LocalDateTime.now()))
                .orderByDesc(RegionRule::getRuleVersion)
                .last("LIMIT 1");
        RegionRule rule = regionRuleMapper.selectOne(wrapper);
        if (rule == null) {
            log.warn("[规则匹配] 未找到地区[{}]类型[{}]的有效规则", regionCode, ruleType);
        }
        return rule;
    }

    @Override
    public List<RegionRule> matchRulesForApplication(TransferApplyDTO dto) {
        List<RegionRule> rules = new ArrayList<>();
        RegionRule outRule = matchRegionRule(dto.getTransferOutRegion(), 1);
        if (outRule != null) rules.add(outRule);
        RegionRule inRule = matchRegionRule(dto.getTransferInRegion(), 2);
        if (inRule != null) rules.add(inRule);
        return rules;
    }

    @Override
    public ValidationResult validateApplication(TransferApplyDTO dto) {
        ValidationResult result = new ValidationResult();
        List<String> errors = new ArrayList<>();

        RegionRule outRule = matchRegionRule(dto.getTransferOutRegion(), 1);
        RegionRule inRule = matchRegionRule(dto.getTransferInRegion(), 2);
        result.setTransferOutRule(outRule);
        result.setTransferInRule(inRule);

        if (outRule == null) {
            errors.add("转出地[" + dto.getTransferOutRegion() + "]未配置转出受理规则");
            result.setRejectReasonCode("R014");
            result.setRejectReasonName("违反属地化管理规定");
        }
        if (inRule == null) {
            errors.add("转入地[" + dto.getTransferInRegion() + "]未配置转入受理规则");
            result.setRejectReasonCode("R014");
            result.setRejectReasonName("违反属地化管理规定");
        }

        if (inRule != null && inRule.getMinContributionMonths() != null) {
        }

        if (dto.getTransferAmount() != null) {
            if (outRule != null && outRule.getMinTransferAmount() != null) {
                if (dto.getTransferAmount().compareTo(outRule.getMinTransferAmount()) < 0) {
                    errors.add("转移金额低于转出地最低限额" + outRule.getMinTransferAmount() + "元");
                }
            }
            if (outRule != null && outRule.getMaxTransferAmount() != null) {
                if (dto.getTransferAmount().compareTo(outRule.getMaxTransferAmount()) > 0) {
                    errors.add("转移金额超过转出地最高限额" + outRule.getMaxTransferAmount() + "元");
                }
            }
            if (outRule != null && outRule.getAllowPartialTransfer() != null
                    && outRule.getAllowPartialTransfer() == 0) {
            }
        }

        if (dto.getTransferOutRegion().equals(dto.getTransferInRegion())) {
            errors.add("转出地与转入地不能相同");
        }

        result.setErrorMessages(errors);
        result.setPassed(CollectionUtils.isEmpty(errors));
        return result;
    }

    @Override
    public DuplicateCheckResult checkDuplicate(TransferApplyDTO dto) {
        return checkDuplicate(dto, null);
    }

    @Override
    public DuplicateCheckResult checkDuplicate(TransferApplyDTO dto, Long excludeApplicationId) {
        DuplicateCheckResult result = new DuplicateCheckResult();

        LambdaQueryWrapper<TransferApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransferApplication::getIdCardNo, dto.getIdCardNo())
                .eq(TransferApplication::getTransferOutRegion, dto.getTransferOutRegion())
                .eq(TransferApplication::getTransferInRegion, dto.getTransferInRegion())
                .in(TransferApplication::getApplicationStatus, PROCESSING_STATUS)
                .ge(TransferApplication::getSubmitTime, LocalDateTime.now().minusDays(90));
        if (excludeApplicationId != null) {
            wrapper.ne(TransferApplication::getId, excludeApplicationId);
        }

        List<TransferApplication> duplicates = applicationMapper.selectList(wrapper);

        if (!CollectionUtils.isEmpty(duplicates)) {
            result.setDuplicate(true);
            result.setDuplicateApplications(duplicates);
            String appNos = duplicates.stream()
                    .map(TransferApplication::getApplicationNo)
                    .collect(Collectors.joining(","));
            result.setDescription("检测到该申请人存在" + duplicates.size()
                    + "笔相同转出/转入地的在途申请，申请编号：" + appNos);
            log.info("[重复检测] 证件号[{}] 检测到重复申请: {}", dto.getIdCardNo(), appNos);
        }

        return result;
    }

    @Override
    public ConflictCheckResult checkConflict(TransferApplyDTO dto) {
        return checkConflict(dto, null);
    }

    @Override
    public ConflictCheckResult checkConflict(TransferApplyDTO dto, Long excludeApplicationId) {
        ConflictCheckResult result = new ConflictCheckResult();
        List<String> reasons = new ArrayList<>();

        LambdaQueryWrapper<TransferApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransferApplication::getIdCardNo, dto.getIdCardNo())
                .in(TransferApplication::getApplicationStatus, PROCESSING_STATUS);
        if (excludeApplicationId != null) {
            wrapper.ne(TransferApplication::getId, excludeApplicationId);
        }
        List<TransferApplication> processingApps = applicationMapper.selectList(wrapper);

        for (TransferApplication app : processingApps) {
            boolean outConflict = app.getTransferOutRegion().equals(dto.getTransferOutRegion())
                    && !app.getTransferInRegion().equals(dto.getTransferInRegion());
            boolean inConflict = app.getTransferInRegion().equals(dto.getTransferInRegion())
                    && !app.getTransferOutRegion().equals(dto.getTransferOutRegion());

            if (outConflict) {
                reasons.add("存在从转出地[" + dto.getTransferOutRegion()
                        + "]转出至其他地区[" + app.getTransferInRegion() + "]的在途申请["
                        + app.getApplicationNo() + "]");
            }
            if (inConflict) {
                reasons.add("存在从其他地区[" + app.getTransferOutRegion()
                        + "]转入至转入地[" + dto.getTransferInRegion() + "]的在途申请["
                        + app.getApplicationNo() + "]");
            }
        }

        if (!reasons.isEmpty()) {
            result.setConflict(true);
            result.setConflictReasons(reasons);
            result.setDescription(String.join("; ", reasons));
        }

        return result;
    }

    @Override
    public RuleValidateVO validateFull(TransferApplication application) {
        RuleValidateVO vo = new RuleValidateVO();
        List<String> passedItems = new ArrayList<>();
        List<String> failedItems = new ArrayList<>();

        RegionRule outRule = matchRegionRule(application.getTransferOutRegion(), 1);
        RegionRule inRule = matchRegionRule(application.getTransferInRegion(), 2);

        if (outRule != null) {
            passedItems.add("转出地规则匹配成功：" + outRule.getRuleName() + "(v" + outRule.getRuleVersion() + ")");
        } else {
            failedItems.add("转出地规则未配置");
        }

        if (inRule != null) {
            passedItems.add("转入地规则匹配成功：" + inRule.getRuleName() + "(v" + inRule.getRuleVersion() + ")");
        } else {
            failedItems.add("转入地规则未配置");
        }

        TransferApplyDTO temp = new TransferApplyDTO();
        temp.setIdCardNo(application.getIdCardNo());
        temp.setTransferOutRegion(application.getTransferOutRegion());
        temp.setTransferInRegion(application.getTransferInRegion());
        temp.setTransferAmount(application.getTransferAmount());

        DuplicateCheckResult dupResult = checkDuplicate(temp, application.getId());
        if (dupResult.isDuplicate()) {
            failedItems.add(dupResult.getDescription());
            vo.setRejectReasonCode("R013");
            vo.setRejectReasonName("重复申请");
        } else {
            passedItems.add("重复申请检测通过");
        }

        ConflictCheckResult conflictResult = checkConflict(temp, application.getId());
        if (conflictResult.isConflict()) {
            failedItems.addAll(conflictResult.getConflictReasons());
        } else {
            passedItems.add("冲突申请检测通过");
        }

        if (application.getTransferOutRegion().equals(application.getTransferInRegion())) {
            failedItems.add("转出地与转入地不能相同");
        } else {
            passedItems.add("转出/转入地校验通过");
        }

        vo.setPassedItems(passedItems);
        vo.setFailedItems(failedItems);
        vo.setWarnings(new ArrayList<>());
        vo.setPassed(CollectionUtils.isEmpty(failedItems));
        if (!vo.isPassed()) {
            vo.setSuggestion("请处理上述" + failedItems.size() + "项问题后重新提交或走补正流程");
        } else {
            vo.setSuggestion("所有校验项通过，可进入协同流转阶段");
        }
        return vo;
    }
}
