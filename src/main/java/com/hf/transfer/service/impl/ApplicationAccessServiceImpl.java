package com.hf.transfer.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hf.transfer.common.BusinessErrorEnum;
import com.hf.transfer.common.BusinessException;
import com.hf.transfer.common.PageResult;
import com.hf.transfer.common.enums.ApplicationStatusEnum;
import com.hf.transfer.common.enums.ChannelTypeEnum;
import com.hf.transfer.common.enums.TaskStatusEnum;
import com.hf.transfer.domain.dto.ApplicationQueryDTO;
import com.hf.transfer.domain.dto.TransferApplyDTO;
import com.hf.transfer.domain.entity.ApplicationMaterial;
import com.hf.transfer.domain.entity.ApplicationStatusLog;
import com.hf.transfer.domain.entity.CollaborationTask;
import com.hf.transfer.domain.entity.RegionInfo;
import com.hf.transfer.domain.entity.TransferApplication;
import com.hf.transfer.domain.vo.ApplicationDetailVO;
import com.hf.transfer.domain.vo.ApplicationListVO;
import com.hf.transfer.domain.vo.MaterialVO;
import com.hf.transfer.domain.vo.ProgressQueryVO;
import com.hf.transfer.domain.vo.ProgressStepVO;
import com.hf.transfer.domain.vo.StatusLogVO;
import com.hf.transfer.domain.vo.TaskSimpleVO;
import com.hf.transfer.mapper.ApplicationMaterialMapper;
import com.hf.transfer.mapper.RegionInfoMapper;
import com.hf.transfer.mapper.TransferApplicationMapper;
import com.hf.transfer.service.ApplicationAccessService;
import com.hf.transfer.service.CollaborationFlowService;
import com.hf.transfer.service.RuleValidationService;
import com.hf.transfer.service.StatusLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationAccessServiceImpl implements ApplicationAccessService {

    private final TransferApplicationMapper applicationMapper;
    private final ApplicationMaterialMapper materialMapper;
    private final RegionInfoMapper regionInfoMapper;
    private final StatusLogService statusLogService;
    private final RuleValidationService ruleValidationService;
    private final CollaborationFlowService collaborationFlowService;

    private static final String OPERATE_TYPE_SUBMIT = "SUBMIT";
    private static final String OPERATE_TYPE_CANCEL = "CANCEL";
    private static final String OPERATE_TYPE_RULE_AUDIT = "RULE_AUDIT";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submitApplication(TransferApplyDTO dto, String operatorId, String operatorName) {
        RuleValidationService.DuplicateCheckResult dupResult = ruleValidationService.checkDuplicate(dto);
        if (dupResult.isDuplicate()) {
            saveDuplicateApplication(dto, dupResult);
            throw new BusinessException(BusinessErrorEnum.APPLICATION_DUPLICATE.getCode(),
                    dupResult.getDescription());
        }

        TransferApplication app = new TransferApplication();
        BeanUtils.copyProperties(dto, app);

        String applicationNo = "HF" + System.currentTimeMillis() + IdUtil.randomNumbers(4);
        app.setApplicationNo(applicationNo);
        app.setApplicationStatus(ApplicationStatusEnum.PENDING_REVIEW.getCode());
        app.setCurrentNode("PENDING_REVIEW");
        app.setCurrentRegion(dto.getTransferInRegion());
        app.setSubmitTime(LocalDateTime.now());
        app.setRejectCount(0);
        app.setSupplementCount(0);
        app.setUrgeCount(0);
        app.setIsDuplicate(0);
        app.setIsConflict(0);
        app.setCreateBy(StrUtil.isBlank(operatorId) ? "SYSTEM" : operatorId);

        fillRegionNames(app);
        app.setExpectedCompleteTime(LocalDateTime.now().plusDays(15));

        applicationMapper.insert(app);

        if (!CollectionUtils.isEmpty(dto.getMaterials())) {
            saveMaterials(app.getId(), app.getApplicationNo(), dto.getMaterials(), 1);
        }

        statusLogService.logStatusChange(app.getId(), app.getApplicationNo(),
                null, ApplicationStatusEnum.PENDING_REVIEW,
                dto.getTransferInRegion(), operatorId, operatorName,
                OPERATE_TYPE_SUBMIT, "转移申请提交成功，进入审核队列",
                "提交渠道:" + getChannelName(dto.getChannelType()),
                null, null);

        log.info("[申请接入] 提交成功 申请编号:{} 申请人:{} 转出地:{} 转入地:{}",
                applicationNo, dto.getApplicantName(),
                dto.getTransferOutRegion(), dto.getTransferInRegion());

        executeRuleAudit(app, operatorId, operatorName);

        return applicationNo;
    }

    private void saveDuplicateApplication(TransferApplyDTO dto,
                                          RuleValidationService.DuplicateCheckResult dupResult) {
        TransferApplication app = new TransferApplication();
        BeanUtils.copyProperties(dto, app);
        String applicationNo = "HFDUP" + System.currentTimeMillis() + IdUtil.randomNumbers(4);
        app.setApplicationNo(applicationNo);
        app.setApplicationStatus(ApplicationStatusEnum.DUPLICATE_REJECTED.getCode());
        app.setCurrentNode("DUPLICATE_REJECTED");
        app.setSubmitTime(LocalDateTime.now());
        app.setIsDuplicate(1);
        if (!CollectionUtils.isEmpty(dupResult.getDuplicateApplications())) {
            app.setDuplicateAppNo(dupResult.getDuplicateApplications().get(0).getApplicationNo());
        }
        app.setCreateBy("SYSTEM");
        fillRegionNames(app);
        applicationMapper.insert(app);
    }

    private void executeRuleAudit(TransferApplication app, String operatorId, String operatorName) {
        try {
            RuleValidationService.RuleValidateVO validateVO = ruleValidationService.validateFull(app);
            ApplicationStatusEnum fromStatus = ApplicationStatusEnum.getByCode(app.getApplicationStatus());

            if (validateVO.isPassed()) {
                app.setApplicationStatus(ApplicationStatusEnum.RULE_PASSED.getCode());
                app.setCurrentNode("RULE_PASSED");
                app.setAuditTime(LocalDateTime.now());
                applicationMapper.updateById(app);

                statusLogService.logStatusChange(app.getId(), app.getApplicationNo(),
                        fromStatus, ApplicationStatusEnum.RULE_PASSED,
                        app.getCurrentRegion(), operatorId, operatorName,
                        OPERATE_TYPE_RULE_AUDIT, "规则校验通过，进入协同流转准备阶段",
                        "通过项:" + validateVO.getPassedItems().size() + "个",
                        null, null);

                collaborationFlowService.createTransferOutTask(app.getId());
            } else {
                if (validateVO.getRejectReasonCode() != null
                        && "R013".equals(validateVO.getRejectReasonCode())) {
                    app.setApplicationStatus(ApplicationStatusEnum.DUPLICATE_REJECTED.getCode());
                    app.setIsDuplicate(1);
                    app.setCurrentNode("DUPLICATE_REJECTED");
                } else {
                    app.setApplicationStatus(ApplicationStatusEnum.RULE_REJECTED.getCode());
                    app.setCurrentNode("RULE_REJECTED");
                }
                app.setAuditTime(LocalDateTime.now());
                app.setConflictReason(validateVO.getSuggestion());
                applicationMapper.updateById(app);

                ApplicationStatusEnum toStatus = ApplicationStatusEnum.getByCode(app.getApplicationStatus());
                statusLogService.logStatusChange(app.getId(), app.getApplicationNo(),
                        fromStatus, toStatus,
                        app.getCurrentRegion(), operatorId, operatorName,
                        OPERATE_TYPE_RULE_AUDIT, "规则校验未通过: " + validateVO.getSuggestion(),
                        "未通过项:" + validateVO.getFailedItems(),
                        null, null);
            }
        } catch (Exception e) {
            log.error("[申请接入] 规则校验异常 申请ID:{}", app.getId(), e);
        }
    }

    @Override
    public ProgressQueryVO queryProgress(String applicationNo, String idCardNo) {
        LambdaQueryWrapper<TransferApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransferApplication::getApplicationNo, applicationNo);
        if (StrUtil.isNotBlank(idCardNo)) {
            wrapper.eq(TransferApplication::getIdCardNo, idCardNo);
        }
        TransferApplication app = applicationMapper.selectOne(wrapper);
        if (app == null) {
            throw new BusinessException(BusinessErrorEnum.APPLICATION_NOT_FOUND);
        }

        ProgressQueryVO vo = new ProgressQueryVO();
        vo.setApplicationNo(app.getApplicationNo());
        vo.setApplicationStatus(app.getApplicationStatus());
        ApplicationStatusEnum statusEnum = ApplicationStatusEnum.getByCode(app.getApplicationStatus());
        if (statusEnum != null) {
            vo.setApplicationStatusName(statusEnum.getName());
            vo.setApplicationStatusDesc(statusEnum.getDescription());
        }
        vo.setCurrentNode(app.getCurrentNode());
        vo.setCurrentRegion(app.getCurrentRegion());
        vo.setCurrentRegionName(getRegionName(app.getCurrentRegion()));

        vo.setApplicantName(maskName(app.getApplicantName()));
        vo.setTransferOutRegionName(getRegionName(app.getTransferOutRegion()));
        vo.setTransferInRegionName(getRegionName(app.getTransferInRegion()));
        vo.setTransferAmount(app.getTransferAmount());

        vo.setSubmitTime(app.getSubmitTime());
        vo.setExpectedCompleteTime(app.getExpectedCompleteTime());
        vo.setCompleteTime(app.getCompleteTime());

        vo.setRejectCount(app.getRejectCount());
        vo.setSupplementCount(app.getSupplementCount());
        vo.setUrgeCount(app.getUrgeCount());

        vo.setSteps(buildProgressSteps(app));

        return vo;
    }

    private List<ProgressStepVO> buildProgressSteps(TransferApplication app) {
        List<ProgressStepVO> steps = new ArrayList<>();
        List<ApplicationStatusLog> logs = statusLogService.queryByApplicationId(app.getId());
        Map<Integer, ApplicationStatusLog> statusToLog = new HashMap<>();
        for (ApplicationStatusLog l : logs) {
            statusToLog.put(l.getToStatus(), l);
        }

        addStep(steps, 10, "SUBMIT", "申请提交", "用户提交转移申请", app.getSubmitTime(), app);
        addStep(steps, 20, "RULE_AUDIT", "规则校验", "平台统一规则校验", app.getAuditTime(), app);
        addStep(steps, 30, "TRANSFER_OUT", "转出受理", "转出地中心确认受理", app.getTransferOutTime(), app);
        addStep(steps, 50, "TRANSFER_IN", "转入确认", "转入地中心确认到账", app.getTransferInTime(), app);
        addStep(steps, 80, "COMPLETE", "办结归档", "转移接续完成归档", app.getCompleteTime(), app);

        int currentStep = 0;
        Integer currentStatus = app.getApplicationStatus();
        for (int i = 0; i < steps.size(); i++) {
            ProgressStepVO step = steps.get(i);
            if (step.getStepTime() != null || currentStatus != null && currentStatus >= getStepThreshold(step.getStepCode())) {
                step.setStepStatus(2);
                step.setStepStatusName("已完成");
                currentStep = i;
            } else if (i == currentStep + 1) {
                step.setStepStatus(1);
                step.setStepStatusName("进行中");
            } else {
                step.setStepStatus(0);
                step.setStepStatusName("待处理");
            }
        }
        return steps;
    }

    private int getStepThreshold(String stepCode) {
        switch (stepCode) {
            case "SUBMIT": return 10;
            case "RULE_AUDIT": return 20;
            case "TRANSFER_OUT": return 40;
            case "TRANSFER_IN": return 60;
            case "COMPLETE": return 80;
            default: return 0;
        }
    }

    private void addStep(List<ProgressStepVO> steps, int statusCode, String code, String name,
                         String desc, LocalDateTime time, TransferApplication app) {
        ProgressStepVO step = new ProgressStepVO();
        step.setStepCode(code);
        step.setStepName(name);
        step.setStepDescription(desc);
        if (time != null) {
            step.setStepTime(time);
        }
        if ("SUBMIT".equals(code)) {
            step.setOperatorRegion(app.getTransferInRegion());
            step.setOperatorRegionName(getRegionName(app.getTransferInRegion()));
        } else if ("RULE_AUDIT".equals(code)) {
            step.setOperatorRegion(app.getTransferInRegion());
            step.setOperatorRegionName(getRegionName(app.getTransferInRegion()));
        } else if ("TRANSFER_OUT".equals(code)) {
            step.setOperatorRegion(app.getTransferOutRegion());
            step.setOperatorRegionName(getRegionName(app.getTransferOutRegion()));
        } else if ("TRANSFER_IN".equals(code)) {
            step.setOperatorRegion(app.getTransferInRegion());
            step.setOperatorRegionName(getRegionName(app.getTransferInRegion()));
        } else {
            step.setOperatorRegion("SYSTEM");
            step.setOperatorRegionName("系统平台");
        }
        steps.add(step);
    }

    @Override
    public ApplicationDetailVO getApplicationDetail(Long id) {
        TransferApplication app = applicationMapper.selectById(id);
        if (app == null) {
            throw new BusinessException(BusinessErrorEnum.APPLICATION_NOT_FOUND);
        }
        ApplicationDetailVO vo = new ApplicationDetailVO();
        BeanUtils.copyProperties(app, vo);

        vo.setChannelTypeName(getChannelName(app.getChannelType()));
        vo.setApplicationStatusName(ApplicationStatusEnum.getByCode(app.getApplicationStatus()) != null
                ? ApplicationStatusEnum.getByCode(app.getApplicationStatus()).getName() : "");
        vo.setApplicationStatusDesc(ApplicationStatusEnum.getByCode(app.getApplicationStatus()) != null
                ? ApplicationStatusEnum.getByCode(app.getApplicationStatus()).getDescription() : "");
        vo.setTransferOutRegionName(getRegionName(app.getTransferOutRegion()));
        vo.setTransferInRegionName(getRegionName(app.getTransferInRegion()));
        vo.setCurrentRegionName(getRegionName(app.getCurrentRegion()));

        LambdaQueryWrapper<ApplicationMaterial> mWrapper = new LambdaQueryWrapper<>();
        mWrapper.eq(ApplicationMaterial::getApplicationId, id)
                .eq(ApplicationMaterial::getIsEffective, 1)
                .orderByAsc(ApplicationMaterial::getMaterialType);
        List<ApplicationMaterial> materials = materialMapper.selectList(mWrapper);
        vo.setMaterials(materials.stream().map(m -> {
            MaterialVO mvo = new MaterialVO();
            BeanUtils.copyProperties(m, mvo);
            return mvo;
        }).collect(Collectors.toList()));

        List<ApplicationStatusLog> statusLogs = statusLogService.queryByApplicationId(id);
        vo.setStatusLogs(statusLogs.stream().map(sl -> {
            StatusLogVO svo = new StatusLogVO();
            BeanUtils.copyProperties(sl, svo);
            svo.setOperateRegionName(getRegionName(sl.getOperateRegion()));
            return svo;
        }).collect(Collectors.toList()));

        List<CollaborationTask> tasks = collaborationFlowService.getApplicationTasks(id);
        vo.setTasks(tasks.stream().map(t -> {
            TaskSimpleVO tvo = new TaskSimpleVO();
            BeanUtils.copyProperties(t, tvo);
            tvo.setTaskTypeName(getTaskTypeName(t.getTaskType()));
            tvo.setTaskDirectionName(t.getTaskDirection() == 1 ? "转出→转入" : "转入→转出");
            tvo.setSourceRegionName(getRegionName(t.getSourceRegion()));
            tvo.setTargetRegionName(getRegionName(t.getTargetRegion()));
            tvo.setTaskStatusName(TaskStatusEnum.getByCode(t.getTaskStatus()) != null
                    ? TaskStatusEnum.getByCode(t.getTaskStatus()).getName() : "");
            return tvo;
        }).collect(Collectors.toList()));

        return vo;
    }

    @Override
    public PageResult<ApplicationListVO> queryApplicationPage(ApplicationQueryDTO dto) {
        LambdaQueryWrapper<TransferApplication> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(dto.getApplicationNo())) {
            wrapper.like(TransferApplication::getApplicationNo, dto.getApplicationNo());
        }
        if (StrUtil.isNotBlank(dto.getChannelOrderNo())) {
            wrapper.like(TransferApplication::getChannelOrderNo, dto.getChannelOrderNo());
        }
        if (dto.getChannelType() != null) {
            wrapper.eq(TransferApplication::getChannelType, dto.getChannelType());
        }
        if (StrUtil.isNotBlank(dto.getApplicantName())) {
            wrapper.like(TransferApplication::getApplicantName, dto.getApplicantName());
        }
        if (StrUtil.isNotBlank(dto.getIdCardNo())) {
            wrapper.eq(TransferApplication::getIdCardNo, dto.getIdCardNo());
        }
        if (StrUtil.isNotBlank(dto.getMobilePhone())) {
            wrapper.like(TransferApplication::getMobilePhone, dto.getMobilePhone());
        }
        if (StrUtil.isNotBlank(dto.getTransferOutRegion())) {
            wrapper.eq(TransferApplication::getTransferOutRegion, dto.getTransferOutRegion());
        }
        if (StrUtil.isNotBlank(dto.getTransferInRegion())) {
            wrapper.eq(TransferApplication::getTransferInRegion, dto.getTransferInRegion());
        }
        if (dto.getApplicationStatus() != null) {
            wrapper.eq(TransferApplication::getApplicationStatus, dto.getApplicationStatus());
        }
        if (dto.getTransferType() != null) {
            wrapper.eq(TransferApplication::getTransferType, dto.getTransferType());
        }
        if (dto.getSubmitTimeStart() != null) {
            wrapper.ge(TransferApplication::getSubmitTime, dto.getSubmitTimeStart());
        }
        if (dto.getSubmitTimeEnd() != null) {
            wrapper.le(TransferApplication::getSubmitTime, dto.getSubmitTimeEnd());
        }
        if (dto.getCompleteTimeStart() != null) {
            wrapper.ge(TransferApplication::getCompleteTime, dto.getCompleteTimeStart());
        }
        if (dto.getCompleteTimeEnd() != null) {
            wrapper.le(TransferApplication::getCompleteTime, dto.getCompleteTimeEnd());
        }
        if (StrUtil.isNotBlank(dto.getCurrentRegion())) {
            wrapper.eq(TransferApplication::getCurrentRegion, dto.getCurrentRegion());
        }
        wrapper.orderByDesc(TransferApplication::getSubmitTime);

        IPage<TransferApplication> page = applicationMapper.selectPage(
                new Page<>(dto.getCurrent(), dto.getSize()), wrapper);

        List<ApplicationListVO> voList = page.getRecords().stream().map(app -> {
            ApplicationListVO vo = new ApplicationListVO();
            BeanUtils.copyProperties(app, vo);
            vo.setChannelTypeName(getChannelName(app.getChannelType()));
            vo.setIdCardNoMasked(maskIdCard(app.getIdCardNo()));
            vo.setMobilePhoneMasked(maskMobile(app.getMobilePhone()));
            vo.setTransferOutRegionName(getRegionName(app.getTransferOutRegion()));
            vo.setTransferInRegionName(getRegionName(app.getTransferInRegion()));
            vo.setApplicationStatusName(ApplicationStatusEnum.getByCode(app.getApplicationStatus()) != null
                    ? ApplicationStatusEnum.getByCode(app.getApplicationStatus()).getName() : "");
            vo.setApplicationStatusDesc(ApplicationStatusEnum.getByCode(app.getApplicationStatus()) != null
                    ? ApplicationStatusEnum.getByCode(app.getApplicationStatus()).getDescription() : "");
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(voList, page.getTotal(), page.getSize(), page.getCurrent());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelApplication(Long id, String operatorId, String operatorName, String reason) {
        TransferApplication app = applicationMapper.selectById(id);
        if (app == null) {
            throw new BusinessException(BusinessErrorEnum.APPLICATION_NOT_FOUND);
        }
        List<Integer> cancellableStatus = Arrays.asList(
                ApplicationStatusEnum.PENDING_REVIEW.getCode(),
                ApplicationStatusEnum.RULE_PASSED.getCode(),
                ApplicationStatusEnum.TRANSFER_OUT_PENDING.getCode(),
                ApplicationStatusEnum.SUPPLEMENT_PENDING.getCode()
        );
        if (!cancellableStatus.contains(app.getApplicationStatus())) {
            throw new BusinessException(BusinessErrorEnum.APPLICATION_STATUS_ERROR,
                    "当前状态不允许取消申请");
        }

        ApplicationStatusEnum fromStatus = ApplicationStatusEnum.getByCode(app.getApplicationStatus());
        app.setApplicationStatus(ApplicationStatusEnum.CANCELLED.getCode());
        app.setCurrentNode("CANCELLED");
        app.setCurrentRegion(null);
        app.setRemark(app.getRemark() == null ? reason : app.getRemark() + "; " + reason);
        app.setOperatorId(operatorId);
        app.setOperatorName(operatorName);
        applicationMapper.updateById(app);

        statusLogService.logStatusChange(app.getId(), app.getApplicationNo(),
                fromStatus, ApplicationStatusEnum.CANCELLED,
                null, operatorId, operatorName,
                OPERATE_TYPE_CANCEL, "申请已取消", reason,
                null, null);

        log.info("[申请取消] 申请[{}] 取消成功 操作人:{} 原因:{}", app.getApplicationNo(), operatorName, reason);
    }

    private void saveMaterials(Long applicationId, String applicationNo,
                               List<TransferApplyDTO.MaterialDTO> materials, int round) {
        for (TransferApplyDTO.MaterialDTO m : materials) {
            ApplicationMaterial entity = new ApplicationMaterial();
            BeanUtils.copyProperties(m, entity);
            entity.setApplicationId(applicationId);
            entity.setApplicationNo(applicationNo);
            entity.setVerifyStatus(0);
            entity.setMaterialRound(round);
            entity.setIsEffective(1);
            materialMapper.insert(entity);
        }
    }

    private void fillRegionNames(TransferApplication app) {
        if (StrUtil.isNotBlank(app.getTransferOutRegion())) {
            RegionInfo outRegion = getRegionInfo(app.getTransferOutRegion());
            if (outRegion != null) {
                app.setTransferOutCenter(outRegion.getCenterName());
            }
        }
        if (StrUtil.isNotBlank(app.getTransferInRegion())) {
            RegionInfo inRegion = getRegionInfo(app.getTransferInRegion());
            if (inRegion != null) {
                app.setTransferInCenter(inRegion.getCenterName());
            }
        }
    }

    private RegionInfo getRegionInfo(String regionCode) {
        LambdaQueryWrapper<RegionInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RegionInfo::getRegionCode, regionCode).last("LIMIT 1");
        return regionInfoMapper.selectOne(wrapper);
    }

    private String getRegionName(String regionCode) {
        if (StrUtil.isBlank(regionCode)) return "";
        RegionInfo info = getRegionInfo(regionCode);
        return info != null ? info.getRegionName() : regionCode;
    }

    private String getChannelName(Integer channelType) {
        ChannelTypeEnum e = ChannelTypeEnum.getByCode(channelType);
        return e != null ? e.getName() : "未知渠道";
    }

    private String getTaskTypeName(Integer taskType) {
        if (taskType == null) return "";
        switch (taskType) {
            case 1: return "转出确认";
            case 2: return "转入确认";
            case 3: return "信息补正";
            case 4: return "退件复核";
            default: return "其他任务";
        }
    }

    private String maskName(String name) {
        if (StrUtil.isBlank(name)) return name;
        if (name.length() <= 1) return name;
        return name.charAt(0) + "*" + (name.length() > 2 ? name.substring(name.length() - 1) : "");
    }

    private String maskIdCard(String idCard) {
        if (StrUtil.isBlank(idCard) || idCard.length() < 8) return idCard;
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    private String maskMobile(String mobile) {
        if (StrUtil.isBlank(mobile) || mobile.length() < 7) return mobile;
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }
}
