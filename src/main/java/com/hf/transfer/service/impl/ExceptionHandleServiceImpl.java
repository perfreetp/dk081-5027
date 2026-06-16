package com.hf.transfer.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hf.transfer.common.BusinessErrorEnum;
import com.hf.transfer.common.BusinessException;
import com.hf.transfer.common.PageResult;
import com.hf.transfer.common.enums.ApplicationStatusEnum;
import com.hf.transfer.common.enums.TaskStatusEnum;
import com.hf.transfer.common.enums.UrgeEscalateLevelEnum;
import com.hf.transfer.common.enums.UrgeTypeEnum;
import com.hf.transfer.domain.dto.TransferApplyDTO;
import com.hf.transfer.domain.entity.ApplicationMaterial;
import com.hf.transfer.domain.entity.CollaborationTask;
import com.hf.transfer.domain.entity.RejectReason;
import com.hf.transfer.domain.entity.RegionInfo;
import com.hf.transfer.domain.entity.SupplementRecord;
import com.hf.transfer.domain.entity.TransferApplication;
import com.hf.transfer.domain.entity.UrgeRecord;
import com.hf.transfer.domain.vo.MaterialVO;
import com.hf.transfer.domain.vo.SupplementDetailVO;
import com.hf.transfer.mapper.ApplicationMaterialMapper;
import com.hf.transfer.mapper.CollaborationTaskMapper;
import com.hf.transfer.mapper.RejectReasonMapper;
import com.hf.transfer.mapper.RegionInfoMapper;
import com.hf.transfer.mapper.SupplementRecordMapper;
import com.hf.transfer.mapper.TransferApplicationMapper;
import com.hf.transfer.mapper.UrgeRecordMapper;
import com.hf.transfer.service.CollaborationFlowService;
import com.hf.transfer.service.ExceptionHandleService;
import com.hf.transfer.service.StatusLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExceptionHandleServiceImpl implements ExceptionHandleService {

    private final CollaborationTaskMapper taskMapper;
    private final UrgeRecordMapper urgeRecordMapper;
    private final RejectReasonMapper rejectReasonMapper;
    private final SupplementRecordMapper supplementRecordMapper;
    private final ApplicationMaterialMapper materialMapper;
    private final TransferApplicationMapper applicationMapper;
    private final RegionInfoMapper regionInfoMapper;
    private final StatusLogService statusLogService;
    private final CollaborationFlowService collaborationFlowService;

    private static final String OPERATE_TYPE_URGE = "URGE_TASK";
    private static final String OPERATE_TYPE_TIMEOUT = "MARK_TIMEOUT";
    private static final String OPERATE_TYPE_REQ_SUPP = "REQUEST_SUPPLEMENT";
    private static final String OPERATE_TYPE_SUB_SUPP = "SUBMIT_SUPPLEMENT";
    private static final String OPERATE_TYPE_AUD_SUPP = "AUDIT_SUPPLEMENT";

    private static final List<Integer> PENDING_STATUS = Arrays.asList(
            TaskStatusEnum.PENDING.getCode(),
            TaskStatusEnum.URGED.getCode()
    );

    @Override
    @Scheduled(cron = "0 0 */2 * * ?")
    public void processTimeoutTasks() {
        log.info("[超时催办] 开始执行定时任务...");
        LocalDateTime now = LocalDateTime.now();
        List<CollaborationTask> timeoutTasks = taskMapper.selectTimeoutTasks(now, PENDING_STATUS);
        if (CollectionUtils.isEmpty(timeoutTasks)) {
            log.info("[超时催办] 无超时任务，定时任务结束");
            return;
        }

        int processed = 0;
        int escalated = 0;
        for (CollaborationTask task : timeoutTasks) {
            try {
                long timeoutDays = task.getDeadlineTime() != null
                        ? Duration.between(task.getDeadlineTime(), now).toDays() : 0;
                if (timeoutDays < 0) timeoutDays = 0;

                Integer escalateLevel = UrgeEscalateLevelEnum.getEscalateLevelByTimeoutDays(timeoutDays);
                Integer urgeType;
                Integer urgeLevel;
                boolean isEscalated = false;
                String escalateTo = null;

                if (escalateLevel >= UrgeEscalateLevelEnum.LEVEL_MINISTRY.getLevel()) {
                    urgeType = 13;
                    urgeLevel = 3;
                    isEscalated = true;
                    escalateTo = "部级监管";
                } else if (escalateLevel >= UrgeEscalateLevelEnum.LEVEL_PROVINCE.getLevel()) {
                    urgeType = 12;
                    urgeLevel = 3;
                    isEscalated = true;
                    escalateTo = "省级监管";
                } else if (escalateLevel >= UrgeEscalateLevelEnum.LEVEL_CENTER.getLevel()) {
                    urgeType = 11;
                    urgeLevel = 3;
                    isEscalated = true;
                    escalateTo = "中心主任";
                } else {
                    urgeType = 1;
                    urgeLevel = 2;
                }

                doUrgeTask(task, urgeType, urgeLevel, null);
                processed++;

                if (isEscalated) {
                    escalated++;
                    UrgeRecord lastUrge = getLastUrgeRecord(task.getId());
                    if (lastUrge != null) {
                        lastUrge.setIsEscalated(true);
                        lastUrge.setEscalateLevel(escalateLevel);
                        lastUrge.setEscalateToRegion(task.getTargetRegion());
                        lastUrge.setEscalateToCenter(escalateTo);
                        urgeRecordMapper.updateById(lastUrge);
                    }
                }
            } catch (Exception e) {
                log.error("[超时催办] 处理任务[{}]异常", task.getTaskNo(), e);
            }
        }
        log.info("[超时催办] 定时任务完成，共处理{}个超时任务，升级{}个", processed, escalated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UrgeRecord urgeTaskManually(Long taskId, Integer urgeLevel, String content,
                                       String operatorId, String operatorName) {
        CollaborationTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(BusinessErrorEnum.TASK_NOT_FOUND);
        }
        return doUrgeTask(task, 2, urgeLevel == null ? 2 : urgeLevel, content, operatorId, operatorName);
    }

    private UrgeRecord doUrgeTask(CollaborationTask task, Integer urgeType, Integer urgeLevel, String content,
                                  String... operatorInfo) {
        TransferApplication app = applicationMapper.selectById(task.getApplicationId());

        UrgeRecord urge = new UrgeRecord();
        urge.setTaskId(task.getId());
        urge.setTaskNo(task.getTaskNo());
        urge.setApplicationId(task.getApplicationId());
        urge.setApplicationNo(task.getApplicationNo());
        urge.setUrgeType(urgeType);
        urge.setUrgeLevel(urgeLevel);
        urge.setTargetRegion(task.getTargetRegion());

        String urgeContent = StrUtil.isBlank(content)
                ? buildDefaultUrgeContent(task, app, urgeLevel)
                : content;
        urge.setUrgeContent(urgeContent);

        urge.setNotifyChannel("SYSTEM,EMAIL");
        urge.setNotifyStatus(1);
        urge.setNotifyResponse("{\"code\":200,\"message\":\"通知已送达\"}");

        if (urgeType == 2 && operatorInfo.length >= 2) {
            urge.setUrgeOperatorId(operatorInfo[0]);
            urge.setUrgeOperatorName(operatorInfo[1]);
        }
        urgeRecordMapper.insert(urge);

        task.setTaskStatus(TaskStatusEnum.URGED.getCode());
        task.setUrgeCount(task.getUrgeCount() == null ? 1 : task.getUrgeCount() + 1);
        if (task.getFirstUrgeTime() == null) {
            task.setFirstUrgeTime(LocalDateTime.now());
        }
        task.setLastUrgeTime(LocalDateTime.now());
        taskMapper.updateById(task);

        if (app != null) {
            app.setUrgeCount(app.getUrgeCount() == null ? 1 : app.getUrgeCount() + 1);
            applicationMapper.updateById(app);

            String operatorName = operatorInfo.length >= 2 ? operatorInfo[1] : "SYSTEM";
            statusLogService.logStatusChange(app.getId(), app.getApplicationNo(),
                    ApplicationStatusEnum.getByCode(app.getApplicationStatus()),
                    ApplicationStatusEnum.getByCode(app.getApplicationStatus()),
                    task.getTargetRegion(),
                    operatorInfo.length >= 2 ? operatorInfo[0] : null,
                    operatorName,
                    OPERATE_TYPE_URGE,
                    (urgeType == 1 ? "系统自动催办" : "人工催办") + "：第" + task.getUrgeCount() + "次催办",
                    urgeContent,
                    task.getId(), null);
        }

        log.warn("[超时催办] 任务[{}] 级别:{} 类型:{} 第{}次催办",
                task.getTaskNo(), urgeLevel, urgeType, task.getUrgeCount());
        return urge;
    }

    private String buildDefaultUrgeContent(CollaborationTask task, TransferApplication app, Integer urgeLevel) {
        String levelName = urgeLevel == 1 ? "温馨提醒" : urgeLevel == 2 ? "重要提醒" : "加急催办";
        String taskTypeName = task.getTaskType() == 1 ? "转出确认" : "转入确认";
        String appName = app != null ? app.getApplicantName() : "申请人";
        return String.format("[%s]【%s】任务：申请编号%s，申请人%s，请于%s前完成%s处理。任务编号：%s。",
                levelName,
                getRegionName(task.getTargetRegion()) + "公积金中心",
                task.getApplicationNo(),
                appName,
                task.getDeadlineTime() != null ? task.getDeadlineTime().toString().replace("T", " ") : "尽快",
                taskTypeName,
                task.getTaskNo());
    }

    @Override
    public List<RejectReason> listRejectReasons(Integer category, Integer scene) {
        LambdaQueryWrapper<RejectReason> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RejectReason::getStatus, 1);
        if (category != null) {
            wrapper.eq(RejectReason::getReasonCategory, category);
        }
        if (scene != null) {
            wrapper.and(w -> w.eq(RejectReason::getApplicableScene, 0)
                    .or()
                    .eq(RejectReason::getApplicableScene, scene));
        }
        wrapper.orderByAsc(RejectReason::getSortOrder, RejectReason::getId);
        return rejectReasonMapper.selectList(wrapper);
    }

    @Override
    public SupplementDetailVO getSupplementDetail(Long supplementId) {
        SupplementRecord record = supplementRecordMapper.selectById(supplementId);
        if (record == null) {
            throw new BusinessException(BusinessErrorEnum.APPLICATION_NOT_FOUND);
        }
        return buildSupplementDetailVO(record);
    }

    @Override
    public SupplementDetailVO getSupplementByApplication(Long applicationId) {
        LambdaQueryWrapper<SupplementRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplementRecord::getApplicationId, applicationId)
                .orderByDesc(SupplementRecord::getSupplementRound, SupplementRecord::getId)
                .last("LIMIT 1");
        SupplementRecord record = supplementRecordMapper.selectOne(wrapper);
        if (record == null) {
            return null;
        }
        return buildSupplementDetailVO(record);
    }

    private SupplementDetailVO buildSupplementDetailVO(SupplementRecord record) {
        SupplementDetailVO vo = new SupplementDetailVO();
        BeanUtils.copyProperties(record, vo);
        vo.setRequestRegionName(getRegionName(record.getRequestRegion()));
        vo.setSupplementStatusName(getSupplementStatusName(record.getSupplementStatus()));
        if (record.getAuditResult() != null) {
            vo.setAuditResultName(record.getAuditResult() == 1 ? "审核通过" : "审核不通过");
        }

        LambdaQueryWrapper<ApplicationMaterial> mWrapper = new LambdaQueryWrapper<>();
        mWrapper.eq(ApplicationMaterial::getApplicationId, record.getApplicationId())
                .eq(ApplicationMaterial::getMaterialRound, record.getSupplementRound() + 1)
                .eq(ApplicationMaterial::getIsEffective, 1);
        List<ApplicationMaterial> materials = materialMapper.selectList(mWrapper);
        vo.setMaterials(materials.stream().map(m -> {
            MaterialVO mvo = new MaterialVO();
            BeanUtils.copyProperties(m, mvo);
            return mvo;
        }).collect(Collectors.toList()));

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requestSupplement(Long applicationId, Long taskId, String requestRegion,
                                  String requestOperatorId, String requestOperatorName,
                                  String requestRemark, String requiredItems, Integer deadlineDays) {
        TransferApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            throw new BusinessException(BusinessErrorEnum.APPLICATION_NOT_FOUND);
        }

        LambdaQueryWrapper<SupplementRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplementRecord::getApplicationId, applicationId);
        Integer maxRound = supplementRecordMapper.selectCount(wrapper).intValue();

        SupplementRecord supplement = new SupplementRecord();
        supplement.setApplicationId(applicationId);
        supplement.setApplicationNo(app.getApplicationNo());
        supplement.setTaskId(taskId);
        supplement.setSupplementRound(maxRound + 1);
        supplement.setRequestRegion(requestRegion);
        supplement.setRequestOperatorId(requestOperatorId);
        supplement.setRequestOperatorName(requestOperatorName);
        supplement.setRequestTime(LocalDateTime.now());
        supplement.setRequestRemark(requestRemark);
        supplement.setRequiredItems(requiredItems);
        supplement.setSupplementStatus(10);
        supplement.setDeadlineTime(LocalDateTime.now().plusDays(deadlineDays != null ? deadlineDays : 7));
        supplementRecordMapper.insert(supplement);

        ApplicationStatusEnum fromStatus = ApplicationStatusEnum.getByCode(app.getApplicationStatus());
        app.setApplicationStatus(ApplicationStatusEnum.SUPPLEMENT_PENDING.getCode());
        app.setCurrentNode("SUPPLEMENT_PENDING");
        app.setCurrentRegion(app.getTransferOutRegion());
        app.setSupplementCount(app.getSupplementCount() == null ? 1 : app.getSupplementCount() + 1);
        applicationMapper.updateById(app);

        statusLogService.logStatusChange(app.getId(), app.getApplicationNo(),
                fromStatus, ApplicationStatusEnum.SUPPLEMENT_PENDING,
                requestRegion, requestOperatorId, requestOperatorName,
                OPERATE_TYPE_REQ_SUPP,
                "要求补正材料（第" + supplement.getSupplementRound() + "轮）",
                requestRemark,
                taskId, null);

        log.info("[补正材料] 申请[{}] 第{}轮补正要求已发出 截止:{}",
                app.getApplicationNo(), supplement.getSupplementRound(), supplement.getDeadlineTime());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitSupplementMaterials(Long applicationId, Long supplementId,
                                          String submitOperatorId, String submitOperatorName,
                                          List<TransferApplyDTO.MaterialDTO> materials) {
        SupplementRecord supplement = supplementId != null
                ? supplementRecordMapper.selectById(supplementId)
                : getLatestSupplement(applicationId);
        if (supplement == null) {
            throw new BusinessException(BusinessErrorEnum.APPLICATION_NOT_FOUND, "未找到补正记录");
        }
        if (!supplement.getSupplementStatus().equals(10)) {
            throw new BusinessException(BusinessErrorEnum.APPLICATION_STATUS_ERROR, "该补正批次已提交，无需重复提交");
        }

        supplement.setSubmitTime(LocalDateTime.now());
        supplement.setSubmitOperatorId(submitOperatorId);
        supplement.setSubmitOperatorName(submitOperatorName);
        supplement.setSupplementStatus(20);
        supplementRecordMapper.updateById(supplement);

        int round = supplement.getSupplementRound() + 1;
        if (!CollectionUtils.isEmpty(materials)) {
            for (TransferApplyDTO.MaterialDTO m : materials) {
                ApplicationMaterial entity = new ApplicationMaterial();
                BeanUtils.copyProperties(m, entity);
                entity.setApplicationId(applicationId);
                entity.setApplicationNo(supplement.getApplicationNo());
                entity.setVerifyStatus(0);
                entity.setMaterialRound(round);
                entity.setIsEffective(1);
                materialMapper.insert(entity);
            }
        }

        TransferApplication app = applicationMapper.selectById(applicationId);
        statusLogService.logStatusChange(app.getId(), app.getApplicationNo(),
                ApplicationStatusEnum.SUPPLEMENT_PENDING, ApplicationStatusEnum.SUPPLEMENT_PENDING,
                supplement.getRequestRegion(), submitOperatorId, submitOperatorName,
                OPERATE_TYPE_SUB_SUPP,
                "补正材料已提交（第" + supplement.getSupplementRound() + "轮）",
                "共提交" + (materials != null ? materials.size() : 0) + "份材料",
                supplement.getTaskId(), null);

        log.info("[补正材料] 申请[{}] 第{}轮补正材料已提交", app.getApplicationNo(), supplement.getSupplementRound());
        return supplement.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditSupplement(Long supplementId, Integer auditResult, String auditRemark,
                                String auditOperatorId, String auditOperatorName) {
        SupplementRecord supplement = supplementRecordMapper.selectById(supplementId);
        if (supplement == null) {
            throw new BusinessException(BusinessErrorEnum.APPLICATION_NOT_FOUND, "未找到补正记录");
        }
        if (!supplement.getSupplementStatus().equals(20)) {
            throw new BusinessException(BusinessErrorEnum.APPLICATION_STATUS_ERROR, "该补正批次状态不允许审核");
        }

        supplement.setAuditTime(LocalDateTime.now());
        supplement.setAuditOperatorId(auditOperatorId);
        supplement.setAuditOperatorName(auditOperatorName);
        supplement.setAuditResult(auditResult);
        supplement.setAuditRemark(auditRemark);
        supplement.setSupplementStatus(auditResult == 1 ? 30 : 40);
        supplementRecordMapper.updateById(supplement);

        TransferApplication app = applicationMapper.selectById(supplement.getApplicationId());
        ApplicationStatusEnum fromStatus = ApplicationStatusEnum.getByCode(app.getApplicationStatus());

        if (auditResult == 1) {
            if (supplement.getTaskId() != null) {
                CollaborationTask task = taskMapper.selectById(supplement.getTaskId());
                if (task != null && TaskStatusEnum.REJECTED.getCode().equals(task.getTaskStatus())) {
                    task.setTaskStatus(TaskStatusEnum.PENDING.getCode());
                    task.setConfirmResult(null);
                    task.setConfirmTime(null);
                    task.setConfirmRemark(null);
                    task.setRejectReasonCode(null);
                    task.setRejectReasonName(null);
                    taskMapper.updateById(task);
                }
            }

            boolean isOutPhase = app.getTransferOutTime() == null;
            ApplicationStatusEnum toStatus = isOutPhase
                    ? ApplicationStatusEnum.TRANSFER_OUT_PENDING
                    : ApplicationStatusEnum.TRANSFER_IN_PENDING;
            app.setApplicationStatus(toStatus.getCode());
            app.setCurrentNode(toStatus.name());
            app.setCurrentRegion(isOutPhase ? app.getTransferOutRegion() : app.getTransferInRegion());
            applicationMapper.updateById(app);

            statusLogService.logStatusChange(app.getId(), app.getApplicationNo(),
                    fromStatus, toStatus,
                    supplement.getRequestRegion(), auditOperatorId, auditOperatorName,
                    OPERATE_TYPE_AUD_SUPP,
                    "补正材料审核通过，重新进入协同流转",
                    auditRemark,
                    supplement.getTaskId(), null);

            log.info("[补正材料] 申请[{}] 第{}轮补正审核通过，重新进入{}阶段",
                    app.getApplicationNo(), supplement.getSupplementRound(), toStatus.getName());
        } else {
            app.setApplicationStatus(ApplicationStatusEnum.SUPPLEMENT_PENDING.getCode());
            app.setSupplementCount(app.getSupplementCount() == null ? 1 : app.getSupplementCount() + 1);
            applicationMapper.updateById(app);

            statusLogService.logStatusChange(app.getId(), app.getApplicationNo(),
                    fromStatus, ApplicationStatusEnum.SUPPLEMENT_PENDING,
                    supplement.getRequestRegion(), auditOperatorId, auditOperatorName,
                    OPERATE_TYPE_AUD_SUPP,
                    "补正材料审核不通过，需继续补正（第" + supplement.getSupplementRound() + "轮）",
                    auditRemark,
                    supplement.getTaskId(), null);

            log.info("[补正材料] 申请[{}] 第{}轮补正审核不通过", app.getApplicationNo(), supplement.getSupplementRound());
        }
    }

    @Override
    public PageResult<UrgeRecord> queryUrgeRecords(Long current, Long size,
                                                   Long taskId, String applicationNo,
                                                   String targetRegion, Integer urgeType,
                                                   LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<UrgeRecord> wrapper = new LambdaQueryWrapper<>();
        if (taskId != null) wrapper.eq(UrgeRecord::getTaskId, taskId);
        if (StrUtil.isNotBlank(applicationNo)) wrapper.like(UrgeRecord::getApplicationNo, applicationNo);
        if (StrUtil.isNotBlank(targetRegion)) wrapper.eq(UrgeRecord::getTargetRegion, targetRegion);
        if (urgeType != null) wrapper.eq(UrgeRecord::getUrgeType, urgeType);
        if (startTime != null) wrapper.ge(UrgeRecord::getCreateTime, startTime);
        if (endTime != null) wrapper.le(UrgeRecord::getCreateTime, endTime);
        wrapper.orderByDesc(UrgeRecord::getCreateTime);

        IPage<UrgeRecord> page = urgeRecordMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(page);
    }

    @Override
    public List<UrgeRecord> getUrgeRecordsByApplication(Long applicationId) {
        LambdaQueryWrapper<UrgeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UrgeRecord::getApplicationId, applicationId)
                .orderByDesc(UrgeRecord::getCreateTime);
        return urgeRecordMapper.selectList(wrapper);
    }

    private UrgeRecord getLastUrgeRecord(Long taskId) {
        LambdaQueryWrapper<UrgeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UrgeRecord::getTaskId, taskId)
                .orderByDesc(UrgeRecord::getCreateTime)
                .last("LIMIT 1");
        return urgeRecordMapper.selectOne(wrapper);
    }

    private SupplementRecord getLatestSupplement(Long applicationId) {
        LambdaQueryWrapper<SupplementRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplementRecord::getApplicationId, applicationId)
                .orderByDesc(SupplementRecord::getSupplementRound, SupplementRecord::getId)
                .last("LIMIT 1");
        return supplementRecordMapper.selectOne(wrapper);
    }

    private String getRegionName(String regionCode) {
        if (StrUtil.isBlank(regionCode)) return "";
        LambdaQueryWrapper<RegionInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RegionInfo::getRegionCode, regionCode).last("LIMIT 1");
        RegionInfo info = regionInfoMapper.selectOne(wrapper);
        return info != null ? info.getRegionName() : regionCode;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UrgeRecord escalateUrgeLevel(Long taskId, Integer escalateLevel, String operatorId, String operatorName) {
        CollaborationTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(BusinessErrorEnum.TASK_NOT_FOUND);
        }
        if (!PENDING_STATUS.contains(task.getTaskStatus()) && !TaskStatusEnum.URGED.getCode().equals(task.getTaskStatus())) {
            throw new BusinessException(BusinessErrorEnum.TASK_STATUS_ERROR, "任务状态不允许催办升级");
        }

        UrgeEscalateLevelEnum levelEnum = UrgeEscalateLevelEnum.getByLevel(escalateLevel);
        if (levelEnum == null) {
            throw new BusinessException(BusinessErrorEnum.PARAM_ERROR, "无效的升级级别，有效值：0普通 1中心主任 2省级 3部级");
        }

        Integer urgeType;
        boolean isEscalated = false;
        switch (escalateLevel) {
            case 0:
                urgeType = 2; // 人工催办
                break;
            case 1:
                urgeType = 11; // 升级至中心主任
                isEscalated = true;
                break;
            case 2:
                urgeType = 12; // 升级至省级监管
                isEscalated = true;
                break;
            case 3:
                urgeType = 13; // 升级至部级监管
                isEscalated = true;
                break;
            default:
                urgeType = 2;
        }

        TransferApplication app = applicationMapper.selectById(task.getApplicationId());
        String customContent = String.format("[%s] 【%s】：申请编号%s，申请人%s，请立即处理%s任务。任务编号：%s。",
                levelEnum.getName(),
                getRegionName(task.getTargetRegion()) + "公积金中心",
                task.getApplicationNo(),
                app != null ? app.getApplicantName() : "申请人",
                task.getTaskType() == 1 ? "转出确认" : "转入确认",
                task.getTaskNo());

        UrgeRecord urgeRecord = doUrgeTask(task, urgeType, escalateLevel, customContent, operatorId, operatorName);

        if (isEscalated) {
            urgeRecord.setIsEscalated(true);
            urgeRecord.setEscalateLevel(escalateLevel);
            urgeRecord.setEscalateToRegion(task.getTargetRegion());
            urgeRecord.setEscalateToCenter(levelEnum.getName());
            urgeRecord.setEscalateTo(getRegionName(task.getTargetRegion()) + " " + levelEnum.getName());
            urgeRecordMapper.updateById(urgeRecord);
        }

        log.info("[催办升级] 任务[{}] 手动升级至级别:{} ({})", task.getTaskNo(), escalateLevel, levelEnum.getName());
        return urgeRecord;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollaborationTask simulateTaskTimeout(Long taskId, Integer timeoutDays) {
        CollaborationTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(BusinessErrorEnum.TASK_NOT_FOUND);
        }
        if (timeoutDays == null || timeoutDays < 0) {
            timeoutDays = 3;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadlineTime = now.minusDays(timeoutDays);
        LocalDateTime assignTime = deadlineTime.minusDays(5);

        task.setAssignTime(assignTime);
        task.setDeadlineTime(deadlineTime);
        task.setIsTimeout(1);
        task.setTimeoutDays(timeoutDays);
        taskMapper.updateById(task);

        TransferApplication app = applicationMapper.selectById(task.getApplicationId());
        if (app != null) {
            app.setIsTimeout(1);
            applicationMapper.updateById(app);
        }

        log.info("[模拟超时] 任务[{}] 设置为已超时 {} 天", task.getTaskNo(), timeoutDays);
        return task;
    }
}
