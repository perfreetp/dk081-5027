package com.hf.transfer.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
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
import com.hf.transfer.common.enums.UrgeEscalateLevelEnum;
import com.hf.transfer.common.enums.UrgeTypeEnum;
import com.hf.transfer.domain.dto.ApplicationQueryDTO;
import com.hf.transfer.domain.dto.TransferApplyDTO;
import com.hf.transfer.domain.entity.ApplicationMaterial;
import com.hf.transfer.domain.entity.ApplicationStatusLog;
import com.hf.transfer.domain.entity.CollaborationTask;
import com.hf.transfer.domain.entity.OperationLog;
import com.hf.transfer.domain.entity.RegionInfo;
import com.hf.transfer.domain.entity.RegionRule;
import com.hf.transfer.domain.entity.RejectReason;
import com.hf.transfer.domain.entity.SupplementRecord;
import com.hf.transfer.domain.entity.TransferApplication;
import com.hf.transfer.domain.entity.UrgeRecord;
import com.hf.transfer.domain.vo.ApplicationCallbackVO;
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

import java.time.Duration;
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
    private final com.hf.transfer.service.ExceptionHandleService exceptionHandleService;
    private final com.hf.transfer.mapper.ApplicationStatusLogMapper statusLogMapper;
    private final com.hf.transfer.mapper.OperationLogMapper operationLogMapper;
    private final com.hf.transfer.mapper.SupplementRecordMapper supplementRecordMapper;
    private final com.hf.transfer.mapper.RejectReasonMapper rejectReasonMapper;
    private final com.hf.transfer.mapper.UrgeRecordMapper urgeRecordMapper;
    private final com.hf.transfer.mapper.CollaborationTaskMapper taskMapper;

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

        String applicationNo = "HF" + System.currentTimeMillis() + RandomUtil.randomNumbers(4);
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
        String applicationNo = "HFDUP" + System.currentTimeMillis() + RandomUtil.randomNumbers(4);
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

        // 催办升级记录
        List<UrgeRecord> urgeRecords = exceptionHandleService.getUrgeRecordsByApplication(app.getId());
        if (urgeRecords != null && !urgeRecords.isEmpty()) {
            List<ApplicationCallbackVO.UrgeRecordVO> urgeVOs = urgeRecords.stream().map(u -> {
                ApplicationCallbackVO.UrgeRecordVO urgeVO = new ApplicationCallbackVO.UrgeRecordVO();
                urgeVO.setUrgeType(u.getUrgeType());
                urgeVO.setUrgeTypeName(UrgeTypeEnum.getNameByCode(u.getUrgeType()));
                urgeVO.setUrgeLevel(u.getUrgeLevel());
                urgeVO.setUrgeLevelName(UrgeEscalateLevelEnum.getNameByCode(u.getUrgeLevel()));
                urgeVO.setUrgeContent(u.getUrgeContent());
                urgeVO.setOperatorName(u.getUrgeOperatorName());
                urgeVO.setUrgeTime(u.getCreateTime());
                urgeVO.setIsEscalated(u.getIsEscalated());
                if (u.getIsEscalated() != null && u.getIsEscalated()) {
                    urgeVO.setEscalateTo(u.getEscalateToRegion() + " " + u.getEscalateToCenter());
                }
                return urgeVO;
            }).collect(Collectors.toList());
            vo.setUrgeLogs(urgeVOs);
        }

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

    @Override
    public com.hf.transfer.domain.vo.ApplicationCallbackVO getCallbackInfo(String applicationNo) {
        LambdaQueryWrapper<TransferApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransferApplication::getApplicationNo, applicationNo);
        TransferApplication app = applicationMapper.selectOne(wrapper);
        if (app == null) {
            throw new BusinessException(BusinessErrorEnum.APPLICATION_NOT_FOUND);
        }

        com.hf.transfer.domain.vo.ApplicationCallbackVO vo = new com.hf.transfer.domain.vo.ApplicationCallbackVO();
        vo.setApplicationNo(app.getApplicationNo());
        vo.setApplicationStatus(app.getApplicationStatus());
        ApplicationStatusEnum statusEnum = ApplicationStatusEnum.getByCode(app.getApplicationStatus());
        if (statusEnum != null) {
            vo.setApplicationStatusName(statusEnum.getName());
            vo.setApplicationStatusDesc(statusEnum.getDescription());
        }
        vo.setCurrentNode(app.getCurrentNode());
        vo.setCurrentRegionCode(app.getCurrentRegion());
        vo.setCurrentRegionName(getRegionName(app.getCurrentRegion()));

        vo.setApplicantName(maskName(app.getApplicantName()));
        vo.setIdCardNo(maskIdCard(app.getIdCardNo()));
        vo.setTransferOutRegionName(getRegionName(app.getTransferOutRegion()));
        vo.setTransferInRegionName(getRegionName(app.getTransferInRegion()));
        vo.setTransferAmount(app.getTransferAmount());
        vo.setActualTransferAmount(app.getActualTransferAmount());

        vo.setSubmitTime(app.getSubmitTime());
        vo.setExpectedCompleteTime(app.getExpectedCompleteTime());
        vo.setCompleteTime(app.getCompleteTime());

        vo.setLastOperation(findLastOperation(app.getId()));
        vo.setNextTodo(findNextTodo(app));

        List<UrgeRecord> urgeRecords = exceptionHandleService.getUrgeRecordsByApplication(app.getId());
        vo.setUrgeRecords(urgeRecords.stream().map(this::convertUrgeRecord).collect(Collectors.toList()));

        boolean isCompleted = ApplicationStatusEnum.COMPLETED.getCode().equals(app.getApplicationStatus())
                || ApplicationStatusEnum.TERMINATED.getCode().equals(app.getApplicationStatus())
                || ApplicationStatusEnum.CANCELLED.getCode().equals(app.getApplicationStatus());
        vo.setIsCompleted(isCompleted);

        if (app.getExpectedCompleteTime() != null) {
            long days = Duration.between(LocalDateTime.now(), app.getExpectedCompleteTime()).toDays();
            vo.setIsTimeout(days < 0);
            vo.setRemainingDays((int) Math.abs(days));
        } else {
            vo.setIsTimeout(false);
            vo.setRemainingDays(null);
        }

        return vo;
    }

    private com.hf.transfer.domain.vo.ApplicationCallbackVO.OperationLogVO findLastOperation(Long applicationId) {
        LambdaQueryWrapper<ApplicationStatusLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApplicationStatusLog::getApplicationId, applicationId)
                .orderByDesc(ApplicationStatusLog::getCreateTime)
                .last("LIMIT 1");
        ApplicationStatusLog log = statusLogMapper.selectOne(wrapper);
        if (log == null) return null;

        com.hf.transfer.domain.vo.ApplicationCallbackVO.OperationLogVO vo = new com.hf.transfer.domain.vo.ApplicationCallbackVO.OperationLogVO();
        vo.setOperateType(log.getOperateType());
        vo.setOperateDesc(log.getOperateDesc());
        vo.setOperatorName(log.getOperatorName());
        vo.setOperateRegionName(getRegionName(log.getOperateRegion()));
        vo.setOperateTime(log.getCreateTime());
        vo.setRemark(log.getOperateRemark());
        return vo;
    }

    private com.hf.transfer.domain.vo.ApplicationCallbackVO.NextTodoVO findNextTodo(TransferApplication app) {
        Integer status = app.getApplicationStatus();
        if (status == null) return null;

        com.hf.transfer.domain.vo.ApplicationCallbackVO.NextTodoVO vo = new com.hf.transfer.domain.vo.ApplicationCallbackVO.NextTodoVO();

        switch (status) {
            case 10:
            case 20:
                vo.setTodoType("AUDIT");
                vo.setTodoName("等待规则校验");
                vo.setHandleRegion(app.getTransferInRegion());
                vo.setHandleRegionName(getRegionName(app.getTransferInRegion()));
                vo.setRequirement("系统自动完成规则校验，无需人工干预");
                break;
            case 30:
            case 40:
                vo.setTodoType("TRANSFER_OUT");
                vo.setTodoName("等待转出地确认");
                vo.setHandleRegion(app.getTransferOutRegion());
                vo.setHandleRegionName(getRegionName(app.getTransferOutRegion()));
                vo.setRequirement("请转出地公积金中心在规定时限内完成转出确认");
                vo.setDeadline(findTaskDeadline(app.getId(), 1));
                break;
            case 50:
            case 60:
                vo.setTodoType("TRANSFER_IN");
                vo.setTodoName("等待转入地确认");
                vo.setHandleRegion(app.getTransferInRegion());
                vo.setHandleRegionName(getRegionName(app.getTransferInRegion()));
                vo.setRequirement("请转入地公积金中心在规定时限内完成到账确认");
                vo.setDeadline(findTaskDeadline(app.getId(), 2));
                break;
            case 70:
                vo.setTodoType("SUPPLEMENT");
                vo.setTodoName("等待补正材料");
                vo.setHandleRegion(app.getTransferOutRegion());
                vo.setHandleRegionName(getRegionName(app.getTransferOutRegion()));
                vo.setRequirement("申请人需补充完善相关材料后重新提交审核");
                SupplementRecord supp = getLatestSupplement(app.getId());
                if (supp != null) {
                    vo.setDeadline(supp.getDeadlineTime());
                }
                break;
            case 80:
                vo.setTodoType("COMPLETED");
                vo.setTodoName("已办结");
                vo.setRequirement("转移接续业务已完成，无需进一步操作");
                break;
            case 90:
                vo.setTodoType("CANCELLED");
                vo.setTodoName("已取消");
                vo.setRequirement("申请已取消，流程终止");
                break;
            case 95:
                vo.setTodoType("TERMINATED");
                vo.setTodoName("已终止");
                vo.setRequirement("申请已终止，流程结束");
                break;
            default:
                vo.setTodoType("UNKNOWN");
                vo.setTodoName("处理中");
                vo.setRequirement("申请正在处理中，请耐心等待");
        }
        return vo;
    }

    private LocalDateTime findTaskDeadline(Long applicationId, Integer taskType) {
        LambdaQueryWrapper<CollaborationTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CollaborationTask::getApplicationId, applicationId)
                .eq(CollaborationTask::getTaskType, taskType)
                .orderByDesc(CollaborationTask::getCreateTime)
                .last("LIMIT 1");
        CollaborationTask task = taskMapper.selectOne(wrapper);
        return task != null ? task.getDeadlineTime() : null;
    }

    private SupplementRecord getLatestSupplement(Long applicationId) {
        LambdaQueryWrapper<SupplementRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplementRecord::getApplicationId, applicationId)
                .orderByDesc(SupplementRecord::getSupplementRound, SupplementRecord::getId)
                .last("LIMIT 1");
        return supplementRecordMapper.selectOne(wrapper);
    }

    private com.hf.transfer.domain.vo.ApplicationCallbackVO.UrgeRecordVO convertUrgeRecord(UrgeRecord urge) {
        com.hf.transfer.domain.vo.ApplicationCallbackVO.UrgeRecordVO vo = new com.hf.transfer.domain.vo.ApplicationCallbackVO.UrgeRecordVO();
        vo.setUrgeType(urge.getUrgeType());
        com.hf.transfer.common.enums.UrgeTypeEnum typeEnum = com.hf.transfer.common.enums.UrgeTypeEnum.getByCode(urge.getUrgeType());
        vo.setUrgeTypeName(typeEnum != null ? typeEnum.getName() : "未知");
        vo.setUrgeLevel(urge.getUrgeLevel());
        vo.setUrgeLevelName(urge.getUrgeLevel() == 1 ? "一般提醒" : urge.getUrgeLevel() == 2 ? "重要提醒" : "加急");
        vo.setUrgeContent(urge.getUrgeContent());
        vo.setOperatorName(urge.getUrgeOperatorName());
        vo.setUrgeTime(urge.getCreateTime());
        vo.setIsEscalated(urge.getIsEscalated() != null && urge.getIsEscalated());
        vo.setEscalateTo(urge.getEscalateToCenter());
        return vo;
    }

    @Override
    public com.hf.transfer.domain.vo.ArchiveSummaryVO getArchiveSummary(String applicationNo) {
        LambdaQueryWrapper<TransferApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransferApplication::getApplicationNo, applicationNo);
        TransferApplication app = applicationMapper.selectOne(wrapper);
        if (app == null) {
            throw new BusinessException(BusinessErrorEnum.APPLICATION_NOT_FOUND);
        }

        com.hf.transfer.domain.vo.ArchiveSummaryVO archive = new com.hf.transfer.domain.vo.ArchiveSummaryVO();
        archive.setArchiveNo("ARC" + System.currentTimeMillis() + RandomUtil.randomNumbers(4));
        archive.setArchiveTime(LocalDateTime.now());

        com.hf.transfer.domain.vo.ArchiveSummaryVO.BasicInfoVO basic = new com.hf.transfer.domain.vo.ArchiveSummaryVO.BasicInfoVO();
        BeanUtils.copyProperties(app, basic);
        ChannelTypeEnum channelTypeEnum = ChannelTypeEnum.getByCode(app.getChannelType());
        basic.setChannelTypeName(channelTypeEnum != null ? channelTypeEnum.getName() : "未知渠道");
        basic.setIdCardTypeName(app.getIdCardType() == 1 ? "居民身份证" : "其他证件");
        basic.setTransferTypeName(app.getTransferType() == 1 ? "异地转移接续" : app.getTransferType() == 2 ? "同城转移" : "跨机构转移");
        basic.setTransferOutRegionName(getRegionName(app.getTransferOutRegion()));
        basic.setTransferInRegionName(getRegionName(app.getTransferInRegion()));
        ApplicationStatusEnum finalStatus = ApplicationStatusEnum.getByCode(app.getApplicationStatus());
        basic.setFinalStatusName(finalStatus != null ? finalStatus.getName() : "");
        archive.setBasicInfo(basic);

        try {
            com.hf.transfer.domain.vo.ArchiveSummaryVO.RuleValidationResultVO ruleResult = new com.hf.transfer.domain.vo.ArchiveSummaryVO.RuleValidationResultVO();
            RuleValidationService.RuleValidateVO validateVO = ruleValidationService.validateFull(app);
            ruleResult.setPassed(validateVO.isPassed());
            RegionRule outRule = ruleValidationService.matchRegionRule(app.getTransferOutRegion(), 1);
            RegionRule inRule = ruleValidationService.matchRegionRule(app.getTransferInRegion(), 2);
            if (outRule != null) {
                ruleResult.setOutRegionRuleName(outRule.getRuleName());
            }
            if (inRule != null) {
                ruleResult.setInRegionRuleName(inRule.getRuleName());
                ruleResult.setMinContributionMonths(inRule.getMinContributionMonths());
            }
            ruleResult.setCheckItems(validateVO.getPassedItems());
            ruleResult.setWarnings(validateVO.getWarnings());
            ruleResult.setIsDuplicate(app.getIsDuplicate() != null && app.getIsDuplicate() == 1);
            ruleResult.setIsConflict(app.getIsConflict() != null && app.getIsConflict() == 1);
            ruleResult.setDuplicateAppNo(app.getDuplicateAppNo());
            ruleResult.setConflictReason(app.getConflictReason());
            ruleResult.setAuditTime(app.getAuditTime());
            archive.setRuleValidationResult(ruleResult);
        } catch (Exception e) {
            log.warn("[归档摘要] 规则校验信息获取失败 申请:{}", applicationNo, e);
        }

        List<CollaborationTask> tasks = collaborationFlowService.getApplicationTasks(app.getId());
        archive.setCollaborationTasks(tasks.stream().map(t -> {
            com.hf.transfer.domain.vo.ArchiveSummaryVO.CollaborationTaskVO taskVO = new com.hf.transfer.domain.vo.ArchiveSummaryVO.CollaborationTaskVO();
            BeanUtils.copyProperties(t, taskVO);
            taskVO.setTaskTypeName(getTaskTypeName(t.getTaskType()));
            taskVO.setSourceRegionName(getRegionName(t.getSourceRegion()));
            taskVO.setTargetRegionName(getRegionName(t.getTargetRegion()));
            TaskStatusEnum taskStatus = TaskStatusEnum.getByCode(t.getTaskStatus());
            taskVO.setTaskStatusName(taskStatus != null ? taskStatus.getName() : "");
            taskVO.setConfirmResultName(t.getConfirmResult() != null ? (t.getConfirmResult() == 1 ? "通过" : "退回") : "");
            return taskVO;
        }).collect(Collectors.toList()));

        List<CollaborationTask> rejectedTasks = tasks.stream()
                .filter(t -> TaskStatusEnum.REJECTED.getCode().equals(t.getTaskStatus()))
                .collect(Collectors.toList());
        archive.setRejectRecords(rejectedTasks.stream().map(t -> {
            com.hf.transfer.domain.vo.ArchiveSummaryVO.RejectRecordVO rejectVO = new com.hf.transfer.domain.vo.ArchiveSummaryVO.RejectRecordVO();
            rejectVO.setTaskNo(t.getTaskNo());
            rejectVO.setRejectReasonCode(t.getRejectReasonCode());
            rejectVO.setRejectReasonName(t.getRejectReasonName());
            if (t.getRejectReasonCode() != null) {
                RejectReason reason = rejectReasonMapper.selectById(t.getRejectReasonCode());
                if (reason != null) {
                    rejectVO.setRejectReasonDetail(reason.getReasonDetail());
                    rejectVO.setSupplementGuide(reason.getSupplementGuide());
                    rejectVO.setNeedSupplement(reason.getIsNeedSupplement() != null && reason.getIsNeedSupplement() == 1);
                }
            }
            rejectVO.setRemark(t.getConfirmRemark());
            rejectVO.setOperatorName(t.getOperatorName());
            rejectVO.setOperateRegion(getRegionName(t.getTargetRegion()));
            rejectVO.setOperateTime(t.getConfirmTime());
            return rejectVO;
        }).collect(Collectors.toList()));

        LambdaQueryWrapper<SupplementRecord> suppWrapper = new LambdaQueryWrapper<>();
        suppWrapper.eq(SupplementRecord::getApplicationId, app.getId())
                .orderByAsc(SupplementRecord::getSupplementRound, SupplementRecord::getId);
        List<SupplementRecord> supplements = supplementRecordMapper.selectList(suppWrapper);
        archive.setSupplementRecords(supplements.stream().map(s -> {
            com.hf.transfer.domain.vo.ArchiveSummaryVO.SupplementRecordVO suppVO = new com.hf.transfer.domain.vo.ArchiveSummaryVO.SupplementRecordVO();
            BeanUtils.copyProperties(s, suppVO);
            suppVO.setRequestRegion(getRegionName(s.getRequestRegion()));
            suppVO.setAuditResultName(s.getAuditResult() != null ? (s.getAuditResult() == 1 ? "审核通过" : "审核不通过") : "");
            suppVO.setSupplementStatusName(getSupplementStatusName(s.getSupplementStatus()));
            return suppVO;
        }).collect(Collectors.toList()));

        LambdaQueryWrapper<UrgeRecord> urgeWrapper = new LambdaQueryWrapper<>();
        urgeWrapper.eq(UrgeRecord::getApplicationId, app.getId())
                .orderByAsc(UrgeRecord::getCreateTime);
        List<UrgeRecord> urges = urgeRecordMapper.selectList(urgeWrapper);
        archive.setUrgeRecords(urges.stream().map(u -> {
            com.hf.transfer.domain.vo.ArchiveSummaryVO.UrgeRecordVO urgeVO = new com.hf.transfer.domain.vo.ArchiveSummaryVO.UrgeRecordVO();
            BeanUtils.copyProperties(u, urgeVO);
            com.hf.transfer.common.enums.UrgeTypeEnum typeEnum = com.hf.transfer.common.enums.UrgeTypeEnum.getByCode(u.getUrgeType());
            urgeVO.setUrgeTypeName(typeEnum != null ? typeEnum.getName() : "");
            urgeVO.setUrgeLevelName(u.getUrgeLevel() == 1 ? "一般提醒" : u.getUrgeLevel() == 2 ? "重要提醒" : "加急");
            urgeVO.setIsEscalated(u.getIsEscalated() != null && u.getIsEscalated());
            urgeVO.setEscalateToRegion(getRegionName(u.getEscalateToRegion()));
            return urgeVO;
        }).collect(Collectors.toList()));

        List<ApplicationStatusLog> statusLogs = statusLogService.queryByApplicationId(app.getId());
        archive.setStatusLogs(statusLogs.stream().map(sl -> {
            com.hf.transfer.domain.vo.ArchiveSummaryVO.StatusLogVO logVO = new com.hf.transfer.domain.vo.ArchiveSummaryVO.StatusLogVO();
            BeanUtils.copyProperties(sl, logVO);
            logVO.setFromStatusName(ApplicationStatusEnum.getByCode(sl.getFromStatus()) != null
                    ? ApplicationStatusEnum.getByCode(sl.getFromStatus()).getName() : "");
            logVO.setToStatusName(ApplicationStatusEnum.getByCode(sl.getToStatus()) != null
                    ? ApplicationStatusEnum.getByCode(sl.getToStatus()).getName() : "");
            logVO.setOperateRegion(getRegionName(sl.getOperateRegion()));
            logVO.setOperateTime(sl.getCreateTime());
            return logVO;
        }).collect(Collectors.toList()));

        LambdaQueryWrapper<OperationLog> opWrapper = new LambdaQueryWrapper<>();
        opWrapper.eq(OperationLog::getBizId, app.getId().toString())
                .or()
                .eq(OperationLog::getBizNo, app.getApplicationNo());
        opWrapper.orderByAsc(OperationLog::getCreateTime);
        List<OperationLog> opLogs = operationLogMapper.selectList(opWrapper);
        archive.setOperationLogs(opLogs.stream().map(ol -> {
            com.hf.transfer.domain.vo.ArchiveSummaryVO.OperationLogVO opVO = new com.hf.transfer.domain.vo.ArchiveSummaryVO.OperationLogVO();
            BeanUtils.copyProperties(ol, opVO);
            return opVO;
        }).collect(Collectors.toList()));

        com.hf.transfer.domain.vo.ArchiveSummaryVO.SummaryStatisticsVO stats = new com.hf.transfer.domain.vo.ArchiveSummaryVO.SummaryStatisticsVO();
        if (app.getSubmitTime() != null && app.getCompleteTime() != null) {
            long minutes = Duration.between(app.getSubmitTime(), app.getCompleteTime()).toMinutes();
            stats.setTotalDurationMinutes(minutes);
            stats.setTotalDurationDays(minutes / 1440);
        }
        if (app.getAuditTime() != null && app.getTransferOutTime() != null) {
            stats.setTransferOutDurationHours((int) Duration.between(app.getAuditTime(), app.getTransferOutTime()).toHours());
        }
        if (app.getTransferOutTime() != null && app.getTransferInTime() != null) {
            stats.setTransferInDurationHours((int) Duration.between(app.getTransferOutTime(), app.getTransferInTime()).toHours());
        }
        stats.setRejectCount(app.getRejectCount() == null ? 0 : app.getRejectCount());
        stats.setSupplementCount(app.getSupplementCount() == null ? 0 : app.getSupplementCount());
        stats.setUrgeCount(app.getUrgeCount() == null ? 0 : app.getUrgeCount());
        stats.setEscalateCount((int) urges.stream().filter(u -> u.getIsEscalated() != null && u.getIsEscalated()).count());
        if (stats.getTotalDurationDays() != null) {
            if (stats.getTotalDurationDays() <= 3) stats.setProcessingEfficiencyLevel("优秀");
            else if (stats.getTotalDurationDays() <= 7) stats.setProcessingEfficiencyLevel("良好");
            else if (stats.getTotalDurationDays() <= 15) stats.setProcessingEfficiencyLevel("合格");
            else stats.setProcessingEfficiencyLevel("超时");
        }
        archive.setStatistics(stats);

        log.info("[归档摘要] 申请[{}] 归档摘要已生成 档案号:{}", app.getApplicationNo(), archive.getArchiveNo());
        return archive;
    }

    private String getSupplementStatusName(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 10: return "待提交";
            case 20: return "已提交待审核";
            case 30: return "审核通过";
            case 40: return "审核不通过";
            default: return "未知状态";
        }
    }
}
