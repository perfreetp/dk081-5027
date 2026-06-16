package com.hf.transfer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hf.transfer.common.enums.ApplicationStatusEnum;
import com.hf.transfer.domain.entity.ApplicationStatusLog;
import com.hf.transfer.mapper.ApplicationStatusLogMapper;
import com.hf.transfer.service.StatusLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatusLogServiceImpl implements StatusLogService {

    private final ApplicationStatusLogMapper statusLogMapper;

    @Override
    public void logStatusChange(Long applicationId, String applicationNo,
                                ApplicationStatusEnum fromStatus, ApplicationStatusEnum toStatus,
                                String operateRegion, String operatorId, String operatorName,
                                String operateType, String operateDesc, String operateRemark,
                                Long taskId, String clientIp) {
        ApplicationStatusLog logEntity = new ApplicationStatusLog();
        logEntity.setApplicationId(applicationId);
        logEntity.setApplicationNo(applicationNo);
        if (fromStatus != null) {
            logEntity.setFromStatus(fromStatus.getCode());
            logEntity.setFromStatusName(fromStatus.getName());
        }
        logEntity.setToStatus(toStatus.getCode());
        logEntity.setToStatusName(toStatus.getName());
        logEntity.setOperateRegion(operateRegion);
        logEntity.setOperatorId(operatorId);
        logEntity.setOperatorName(operatorName);
        logEntity.setOperateType(operateType);
        logEntity.setOperateDesc(operateDesc);
        logEntity.setOperateRemark(operateRemark);
        logEntity.setTaskId(taskId);
        logEntity.setClientIp(clientIp);
        statusLogMapper.insert(logEntity);
        log.info("[状态日志] 申请[{}] 状态变更: {} -> {}, 操作: {}",
                applicationNo,
                fromStatus != null ? fromStatus.getName() : "无",
                toStatus.getName(),
                operateDesc);
    }

    @Override
    public List<ApplicationStatusLog> queryByApplicationId(Long applicationId) {
        LambdaQueryWrapper<ApplicationStatusLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApplicationStatusLog::getApplicationId, applicationId)
                .orderByAsc(ApplicationStatusLog::getCreateTime);
        return statusLogMapper.selectList(wrapper);
    }
}
