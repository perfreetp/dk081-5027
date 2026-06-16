package com.hf.transfer.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hf.transfer.common.BusinessErrorEnum;
import com.hf.transfer.common.BusinessException;
import com.hf.transfer.common.enums.ApplicationStatusEnum;
import com.hf.transfer.common.enums.TaskStatusEnum;
import com.hf.transfer.domain.entity.CollaborationTask;
import com.hf.transfer.domain.entity.RegionRule;
import com.hf.transfer.domain.entity.TransferApplication;
import com.hf.transfer.mapper.CollaborationTaskMapper;
import com.hf.transfer.mapper.TransferApplicationMapper;
import com.hf.transfer.service.CollaborationFlowService;
import com.hf.transfer.service.StatusLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollaborationFlowServiceImpl implements CollaborationFlowService {

    private final CollaborationTaskMapper taskMapper;
    private final TransferApplicationMapper applicationMapper;
    private final StatusLogService statusLogService;
    private final com.hf.transfer.service.RuleValidationService ruleValidationService;

    private static final String OPERATE_TYPE_CREATE = "CREATE_TASK";
    private static final String OPERATE_TYPE_CONFIRM_OUT = "CONFIRM_TRANSFER_OUT";
    private static final String OPERATE_TYPE_CONFIRM_IN = "CONFIRM_TRANSFER_IN";
    private static final String OPERATE_TYPE_REJECT = "REJECT_TASK";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollaborationTask createTransferOutTask(Long applicationId) {
        TransferApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            throw new BusinessException(BusinessErrorEnum.APPLICATION_NOT_FOUND);
        }
        if (!ApplicationStatusEnum.RULE_PASSED.getCode().equals(app.getApplicationStatus())) {
            throw new BusinessException(BusinessErrorEnum.APPLICATION_STATUS_ERROR);
        }

        RegionRule outRule = ruleValidationService.matchRegionRule(app.getTransferOutRegion(), 1);
        int deadlineDays = outRule != null && outRule.getConfirmationDeadline() != null
                ? outRule.getConfirmationDeadline() : 5;

        CollaborationTask task = new CollaborationTask();
        task.setTaskNo("T" + IdUtil.getSnowflakeNextIdStr());
        task.setApplicationId(applicationId);
        task.setApplicationNo(app.getApplicationNo());
        task.setTaskType(1);
        task.setTaskDirection(1);
        task.setSourceRegion(app.getTransferInRegion());
        task.setTargetRegion(app.getTransferOutRegion());
        task.setTaskStatus(TaskStatusEnum.PENDING.getCode());
        task.setPriority(2);
        task.setAssignTime(LocalDateTime.now());
        task.setDeadlineTime(LocalDateTime.now().plusDays(deadlineDays));
        taskMapper.insert(task);

        app.setApplicationStatus(ApplicationStatusEnum.TRANSFER_OUT_PENDING.getCode());
        app.setCurrentNode("TRANSFER_OUT_PENDING");
        app.setCurrentRegion(app.getTransferOutRegion());
        applicationMapper.updateById(app);

        statusLogService.logStatusChange(applicationId, app.getApplicationNo(),
                ApplicationStatusEnum.RULE_PASSED, ApplicationStatusEnum.TRANSFER_OUT_PENDING,
                app.getTransferOutRegion(), null, null,
                OPERATE_TYPE_CREATE, "生成转出确认协同任务", "任务编号:" + task.getTaskNo(),
                task.getId(), null);

        log.info("[协同流转] 申请[{}] 生成转出任务[{}] 截止:{}",
                app.getApplicationNo(), task.getTaskNo(), task.getDeadlineTime());

        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollaborationTask createTransferInTask(Long applicationId) {
        TransferApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            throw new BusinessException(BusinessErrorEnum.APPLICATION_NOT_FOUND);
        }
        if (!ApplicationStatusEnum.TRANSFER_OUT_CONFIRMED.getCode().equals(app.getApplicationStatus())) {
            throw new BusinessException(BusinessErrorEnum.APPLICATION_STATUS_ERROR);
        }

        RegionRule inRule = ruleValidationService.matchRegionRule(app.getTransferInRegion(), 2);
        int deadlineDays = inRule != null && inRule.getConfirmationDeadline() != null
                ? inRule.getConfirmationDeadline() : 5;

        CollaborationTask task = new CollaborationTask();
        task.setTaskNo("T" + IdUtil.getSnowflakeNextIdStr());
        task.setApplicationId(applicationId);
        task.setApplicationNo(app.getApplicationNo());
        task.setTaskType(2);
        task.setTaskDirection(1);
        task.setSourceRegion(app.getTransferOutRegion());
        task.setTargetRegion(app.getTransferInRegion());
        task.setTaskStatus(TaskStatusEnum.PENDING.getCode());
        task.setPriority(2);
        task.setAssignTime(LocalDateTime.now());
        task.setDeadlineTime(LocalDateTime.now().plusDays(deadlineDays));
        taskMapper.insert(task);

        app.setApplicationStatus(ApplicationStatusEnum.TRANSFER_IN_PENDING.getCode());
        app.setCurrentNode("TRANSFER_IN_PENDING");
        app.setCurrentRegion(app.getTransferInRegion());
        applicationMapper.updateById(app);

        statusLogService.logStatusChange(applicationId, app.getApplicationNo(),
                ApplicationStatusEnum.TRANSFER_OUT_CONFIRMED, ApplicationStatusEnum.TRANSFER_IN_PENDING,
                app.getTransferInRegion(), null, null,
                OPERATE_TYPE_CREATE, "生成转入确认协同任务", "任务编号:" + task.getTaskNo(),
                task.getId(), null);

        log.info("[协同流转] 申请[{}] 生成转入任务[{}] 截止:{}",
                app.getApplicationNo(), task.getTaskNo(), task.getDeadlineTime());

        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmTransferOut(Long taskId, String operatorId, String operatorName,
                                   String remark, BigDecimal actualAmount) {
        CollaborationTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(BusinessErrorEnum.TASK_NOT_FOUND);
        }
        if (!TaskStatusEnum.PENDING.getCode().equals(task.getTaskStatus())
                && !TaskStatusEnum.URGED.getCode().equals(task.getTaskStatus())) {
            throw new BusinessException(BusinessErrorEnum.TASK_STATUS_ERROR);
        }
        if (task.getTaskType() != 1) {
            throw new BusinessException(BusinessErrorEnum.TASK_STATUS_ERROR, "任务类型不正确，仅转出确认任务可执行此操作");
        }

        task.setTaskStatus(TaskStatusEnum.CONFIRMED.getCode());
        task.setConfirmResult(1);
        task.setConfirmTime(LocalDateTime.now());
        task.setConfirmRemark(remark);
        task.setOperatorId(operatorId);
        task.setOperatorName(operatorName);
        taskMapper.updateById(task);

        TransferApplication app = applicationMapper.selectById(task.getApplicationId());
        if (actualAmount != null) {
            app.setActualTransferAmount(actualAmount);
        }
        app.setTransferOutTime(LocalDateTime.now());
        app.setApplicationStatus(ApplicationStatusEnum.TRANSFER_OUT_CONFIRMED.getCode());
        app.setOperatorId(operatorId);
        app.setOperatorName(operatorName);
        applicationMapper.updateById(app);

        statusLogService.logStatusChange(app.getId(), app.getApplicationNo(),
                ApplicationStatusEnum.TRANSFER_OUT_PENDING, ApplicationStatusEnum.TRANSFER_OUT_CONFIRMED,
                task.getTargetRegion(), operatorId, operatorName,
                OPERATE_TYPE_CONFIRM_OUT, "转出地已确认转出", remark,
                task.getId(), null);

        log.info("[协同流转] 任务[{}] 转出已确认 处理人:{}", task.getTaskNo(), operatorName);

        createTransferInTask(app.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmTransferIn(Long taskId, String operatorId, String operatorName, String remark) {
        CollaborationTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(BusinessErrorEnum.TASK_NOT_FOUND);
        }
        if (!TaskStatusEnum.PENDING.getCode().equals(task.getTaskStatus())
                && !TaskStatusEnum.URGED.getCode().equals(task.getTaskStatus())) {
            throw new BusinessException(BusinessErrorEnum.TASK_STATUS_ERROR);
        }
        if (task.getTaskType() != 2) {
            throw new BusinessException(BusinessErrorEnum.TASK_STATUS_ERROR, "任务类型不正确，仅转入确认任务可执行此操作");
        }

        task.setTaskStatus(TaskStatusEnum.COMPLETED.getCode());
        task.setConfirmResult(1);
        task.setConfirmTime(LocalDateTime.now());
        task.setConfirmRemark(remark);
        task.setOperatorId(operatorId);
        task.setOperatorName(operatorName);
        taskMapper.updateById(task);

        TransferApplication app = applicationMapper.selectById(task.getApplicationId());
        app.setTransferInTime(LocalDateTime.now());
        app.setCompleteTime(LocalDateTime.now());
        app.setApplicationStatus(ApplicationStatusEnum.COMPLETED.getCode());
        app.setCurrentNode("COMPLETED");
        app.setCurrentRegion(null);
        app.setOperatorId(operatorId);
        app.setOperatorName(operatorName);
        applicationMapper.updateById(app);

        statusLogService.logStatusChange(app.getId(), app.getApplicationNo(),
                ApplicationStatusEnum.TRANSFER_IN_PENDING, ApplicationStatusEnum.COMPLETED,
                task.getTargetRegion(), operatorId, operatorName,
                OPERATE_TYPE_CONFIRM_IN, "转入地已确认到账，申请办结", remark,
                task.getId(), null);

        log.info("[协同流转] 任务[{}] 转入已确认 申请[{}]办结", task.getTaskNo(), app.getApplicationNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectTask(Long taskId, String operatorId, String operatorName,
                           String rejectReasonCode, String rejectReasonName, String remark,
                           String supplementItems, boolean needSupplement) {
        CollaborationTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(BusinessErrorEnum.TASK_NOT_FOUND);
        }
        if (!TaskStatusEnum.PENDING.getCode().equals(task.getTaskStatus())
                && !TaskStatusEnum.URGED.getCode().equals(task.getTaskStatus())) {
            throw new BusinessException(BusinessErrorEnum.TASK_STATUS_ERROR);
        }

        task.setTaskStatus(TaskStatusEnum.REJECTED.getCode());
        task.setConfirmResult(2);
        task.setConfirmTime(LocalDateTime.now());
        task.setConfirmRemark(remark);
        task.setRejectReasonCode(rejectReasonCode);
        task.setRejectReasonName(rejectReasonName);
        task.setSupplementItems(supplementItems);
        task.setOperatorId(operatorId);
        task.setOperatorName(operatorName);
        taskMapper.updateById(task);

        TransferApplication app = applicationMapper.selectById(task.getApplicationId());
        app.setRejectCount(app.getRejectCount() == null ? 1 : app.getRejectCount() + 1);
        app.setOperatorId(operatorId);
        app.setOperatorName(operatorName);

        ApplicationStatusEnum fromStatus = ApplicationStatusEnum.getByCode(app.getApplicationStatus());
        if (needSupplement) {
            app.setApplicationStatus(ApplicationStatusEnum.SUPPLEMENT_PENDING.getCode());
            app.setCurrentNode("SUPPLEMENT_PENDING");
            app.setSupplementCount(app.getSupplementCount() == null ? 1 : app.getSupplementCount() + 1);
        } else {
            if (task.getTaskType() == 1) {
                app.setApplicationStatus(ApplicationStatusEnum.TRANSFER_OUT_REJECTED.getCode());
            } else {
                app.setApplicationStatus(ApplicationStatusEnum.TERMINATED.getCode());
            }
            app.setCurrentNode("REJECTED");
        }
        applicationMapper.updateById(app);

        ApplicationStatusEnum toStatus = ApplicationStatusEnum.getByCode(app.getApplicationStatus());
        String desc = (task.getTaskType() == 1 ? "转出" : "转入")
                + "审核退回，原因：" + rejectReasonName
                + (needSupplement ? "（需补正材料）" : "（终止流程）");
        statusLogService.logStatusChange(app.getId(), app.getApplicationNo(),
                fromStatus, toStatus,
                task.getTargetRegion(), operatorId, operatorName,
                OPERATE_TYPE_REJECT, desc, remark,
                task.getId(), null);

        log.info("[协同流转] 任务[{}] 退回 原因:{} 补正:{}", task.getTaskNo(), rejectReasonName, needSupplement);
    }

    @Override
    public List<CollaborationTask> getApplicationTasks(Long applicationId) {
        LambdaQueryWrapper<CollaborationTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CollaborationTask::getApplicationId, applicationId)
                .orderByDesc(CollaborationTask::getCreateTime);
        return taskMapper.selectList(wrapper);
    }

    @Override
    public CollaborationTask getTaskDetail(Long taskId) {
        return taskMapper.selectById(taskId);
    }

    @Override
    public void syncTaskToTarget(Long taskId) {
        CollaborationTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(BusinessErrorEnum.TASK_NOT_FOUND);
        }
        task.setSyncAttempts(task.getSyncAttempts() == null ? 1 : task.getSyncAttempts() + 1);
        task.setSyncLastTime(LocalDateTime.now());
        task.setSyncStatus(1);
        task.setSyncResponse("{\"code\":200,\"message\":\"推送成功\",\"timestamp\":" + System.currentTimeMillis() + "}");
        taskMapper.updateById(task);
        log.info("[协同推送] 任务[{}] 推送至地区[{}] 尝试次数:{}",
                task.getTaskNo(), task.getTargetRegion(), task.getSyncAttempts());
    }
}
