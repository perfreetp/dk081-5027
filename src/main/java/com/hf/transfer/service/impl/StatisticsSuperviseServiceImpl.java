package com.hf.transfer.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hf.transfer.common.enums.ApplicationStatusEnum;
import com.hf.transfer.common.enums.ChannelTypeEnum;
import com.hf.transfer.common.enums.TaskStatusEnum;
import com.hf.transfer.domain.entity.CollaborationTask;
import com.hf.transfer.domain.entity.RegionInfo;
import com.hf.transfer.domain.entity.TransferApplication;
import com.hf.transfer.domain.entity.UrgeRecord;
import com.hf.transfer.domain.vo.*;
import com.hf.transfer.mapper.CollaborationTaskMapper;
import com.hf.transfer.mapper.RegionInfoMapper;
import com.hf.transfer.mapper.TransferApplicationMapper;
import com.hf.transfer.mapper.UrgeRecordMapper;
import com.hf.transfer.service.StatisticsSuperviseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsSuperviseServiceImpl implements StatisticsSuperviseService {

    private final TransferApplicationMapper applicationMapper;
    private final CollaborationTaskMapper taskMapper;
    private final RegionInfoMapper regionInfoMapper;
    private final UrgeRecordMapper urgeRecordMapper;

    private static final List<Integer> PROCESSING_STATUS = Arrays.asList(10, 20, 30, 40, 50, 70);
    private static final List<Integer> COMPLETED_STATUS = Arrays.asList(60, 80);
    private static final List<Integer> REJECTED_STATUS = Arrays.asList(25, 28, 45, 90, 95);
    private static final List<Integer> TIMEOUT_TASK_STATUS = Arrays.asList(50, 60);

    @Override
    public StatisticsOverviewVO getOverview(LocalDateTime startTime, LocalDateTime endTime) {
        StatisticsOverviewVO vo = new StatisticsOverviewVO();

        if (startTime == null) startTime = LocalDate.now().minusDays(30).atStartOfDay();
        if (endTime == null) endTime = LocalDateTime.now();

        vo.setPeriodLabel(startTime.toLocalDate() + " 至 " + endTime.toLocalDate());

        LambdaQueryWrapper<TransferApplication> baseWrapper = new LambdaQueryWrapper<>();
        baseWrapper.between(TransferApplication::getSubmitTime, startTime, endTime);

        List<TransferApplication> all = applicationMapper.selectList(baseWrapper);
        vo.setTotalApplyCount((long) all.size());
        vo.setCompletedCount(countByStatus(all, COMPLETED_STATUS));
        vo.setProcessingCount(countByStatus(all, PROCESSING_STATUS));
        vo.setRejectedCount(countByStatus(all, REJECTED_STATUS));
        vo.setSupplementCount(all.stream().filter(a -> a.getSupplementCount() != null && a.getSupplementCount() > 0).count());
        vo.setDuplicateCount(all.stream().filter(a -> a.getIsDuplicate() != null && a.getIsDuplicate() == 1).count());

        vo.setTotalTransferAmount(all.stream()
                .filter(a -> a.getActualTransferAmount() != null)
                .map(TransferApplication::getActualTransferAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        LambdaQueryWrapper<TransferApplication> backlogWrapper = new LambdaQueryWrapper<>();
        backlogWrapper.in(TransferApplication::getApplicationStatus, PROCESSING_STATUS);
        List<TransferApplication> backlogList = applicationMapper.selectList(backlogWrapper);
        vo.setBacklogCount((long) backlogList.size());

        LambdaQueryWrapper<CollaborationTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.lt(CollaborationTask::getDeadlineTime, LocalDateTime.now())
                .in(CollaborationTask::getTaskStatus, TIMEOUT_TASK_STATUS);
        vo.setTimeoutCount((long) taskMapper.selectCount(taskWrapper).intValue());

        long total = vo.getTotalApplyCount() == 0 ? 1 : vo.getTotalApplyCount();
        vo.setCompletionRate(pct(vo.getCompletedCount(), total));
        vo.setRejectionRate(pct(vo.getRejectedCount(), total));
        vo.setTimeoutRate(pct(vo.getTimeoutCount(), total));
        vo.setSupplementRate(pct(vo.getSupplementCount(), total));

        List<TransferApplication> completedList = all.stream()
                .filter(a -> COMPLETED_STATUS.contains(a.getApplicationStatus())
                        && a.getSubmitTime() != null && a.getCompleteTime() != null)
                .collect(Collectors.toList());
        if (!completedList.isEmpty()) {
            double avgDays = completedList.stream()
                    .mapToLong(a -> Duration.between(a.getSubmitTime(), a.getCompleteTime()).toMinutes())
                    .average().orElse(0.0) / 1440.0;
            vo.setAvgProcessingDays(BigDecimal.valueOf(avgDays).setScale(2, RoundingMode.HALF_UP));

            BigDecimal totalAmt = completedList.stream()
                    .filter(a -> a.getActualTransferAmount() != null)
                    .map(TransferApplication::getActualTransferAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setAvgTransferAmount(totalAmt.divide(BigDecimal.valueOf(completedList.size()), 2, RoundingMode.HALF_UP));
        } else {
            vo.setAvgProcessingDays(BigDecimal.ZERO);
            vo.setAvgTransferAmount(BigDecimal.ZERO);
        }

        vo.setTrends(buildKpiTrends(startTime, endTime));

        return vo;
    }

    @Override
    public List<RegionStatisticsVO> getRegionStatistics(LocalDateTime startTime, LocalDateTime endTime,
                                                         String regionCode, Integer roleType) {
        if (startTime == null) startTime = LocalDate.now().minusDays(30).atStartOfDay();
        if (endTime == null) endTime = LocalDateTime.now();
        if (roleType == null) roleType = 0;

        List<RegionInfo> regions;
        if (StrUtil.isNotBlank(regionCode)) {
            RegionInfo r = getRegionByCode(regionCode);
            regions = r != null ? Collections.singletonList(r) : Collections.emptyList();
        } else {
            regions = regionInfoMapper.selectList(new LambdaQueryWrapper<RegionInfo>()
                    .eq(RegionInfo::getStatus, 1)
                    .orderByAsc(RegionInfo::getSortOrder));
        }

        List<RegionStatisticsVO> result = new ArrayList<>();
        int rank = 0;
        for (RegionInfo region : regions) {
            RegionStatisticsVO vo = new RegionStatisticsVO();
            vo.setRegionCode(region.getRegionCode());
            vo.setRegionName(region.getRegionName());
            vo.setCenterName(region.getCenterName());
            vo.setRoleType(roleType);

            LambdaQueryWrapper<TransferApplication> wrapper = new LambdaQueryWrapper<>();
            wrapper.between(TransferApplication::getSubmitTime, startTime, endTime);
            if (roleType == 1 || roleType == 0) {
                wrapper.eq(TransferApplication::getTransferOutRegion, region.getRegionCode());
            } else {
                wrapper.eq(TransferApplication::getTransferInRegion, region.getRegionCode());
            }
            List<TransferApplication> regionApps = applicationMapper.selectList(wrapper);

            vo.setTotalCount((long) regionApps.size());
            vo.setCompletedCount(countByStatus(regionApps, COMPLETED_STATUS));
            vo.setProcessingCount(countByStatus(regionApps, PROCESSING_STATUS));
            vo.setBacklogCount(regionApps.stream()
                    .filter(a -> PROCESSING_STATUS.contains(a.getApplicationStatus())).count());
            vo.setRejectedCount(countByStatus(regionApps, REJECTED_STATUS));
            vo.setSupplementCount(regionApps.stream()
                    .filter(a -> a.getSupplementCount() != null && a.getSupplementCount() > 0).count());

            LambdaQueryWrapper<CollaborationTask> timeoutWrapper = new LambdaQueryWrapper<>();
            timeoutWrapper.eq(roleType == 1 ? CollaborationTask::getSourceRegion :
                            (roleType == 2 ? CollaborationTask::getTargetRegion : CollaborationTask::getTargetRegion),
                    region.getRegionCode())
                    .in(CollaborationTask::getTaskStatus, TIMEOUT_TASK_STATUS)
                    .between(CollaborationTask::getAssignTime, startTime, endTime);
            vo.setTimeoutCount((long) taskMapper.selectCount(timeoutWrapper).intValue());

            long total = vo.getTotalCount() == 0 ? 1 : vo.getTotalCount();
            vo.setCompletionRate(pct(vo.getCompletedCount(), total));
            vo.setRejectionRate(pct(vo.getRejectedCount(), total));
            vo.setTimeoutRate(pct(vo.getTimeoutCount(), total));

            List<TransferApplication> completed = regionApps.stream()
                    .filter(a -> COMPLETED_STATUS.contains(a.getApplicationStatus())
                            && a.getSubmitTime() != null && a.getCompleteTime() != null)
                    .collect(Collectors.toList());
            if (!completed.isEmpty()) {
                double avgDays = completed.stream()
                        .mapToLong(a -> Duration.between(a.getSubmitTime(), a.getCompleteTime()).toMinutes())
                        .average().orElse(0.0) / 1440.0;
                vo.setAvgProcessingDays(BigDecimal.valueOf(avgDays).setScale(2, RoundingMode.HALF_UP));
            } else {
                vo.setAvgProcessingDays(BigDecimal.ZERO);
            }

            vo.setTotalTransferAmount(regionApps.stream()
                    .filter(a -> a.getActualTransferAmount() != null)
                    .map(TransferApplication::getActualTransferAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            result.add(vo);
        }

        result.sort((a, b) -> Long.compare(b.getTotalCount(), a.getTotalCount()));
        for (RegionStatisticsVO vo : result) {
            rank++;
            vo.setRank(rank);
            if (rank <= 3) vo.setRankLevel("A");
            else if (rank <= 7) vo.setRankLevel("B");
            else vo.setRankLevel("C");
        }

        return result;
    }

    @Override
    public List<TimeStatisticsVO> getTimeStatistics(LocalDateTime startTime, LocalDateTime endTime,
                                                     String granularity) {
        if (startTime == null) startTime = LocalDate.now().minusDays(30).atStartOfDay();
        if (endTime == null) endTime = LocalDateTime.now();
        if (StrUtil.isBlank(granularity)) granularity = "DAY";

        List<TimeStatisticsVO> result = new ArrayList<>();
        List<LocalDateTime[]> periods = generatePeriods(startTime, endTime, granularity);
        DateTimeFormatter fmt = getFormatter(granularity);

        for (LocalDateTime[] period : periods) {
            TimeStatisticsVO vo = new TimeStatisticsVO();
            vo.setTimePeriod(period[0].format(fmt));
            vo.setTimeLabel(period[0].format(fmt) + (granularity.equals("HOUR") ? "时" :
                    granularity.equals("WEEK") ? "周" : granularity.equals("MONTH") ? "月" : "日"));

            LambdaQueryWrapper<TransferApplication> wrapper = new LambdaQueryWrapper<>();
            wrapper.between(TransferApplication::getSubmitTime, period[0], period[1]);
            List<TransferApplication> list = applicationMapper.selectList(wrapper);

            vo.setTotalCount((long) list.size());
            vo.setCompletedCount(countByStatus(list, COMPLETED_STATUS));
            vo.setProcessingCount(countByStatus(list, PROCESSING_STATUS));
            vo.setRejectedCount(countByStatus(list, REJECTED_STATUS));
            vo.setCompletionRate(pct(vo.getCompletedCount(), vo.getTotalCount() == 0 ? 1 : vo.getTotalCount()));
            vo.setTotalTransferAmount(list.stream()
                    .filter(a -> a.getActualTransferAmount() != null)
                    .map(TransferApplication::getActualTransferAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            result.add(vo);
        }
        return result;
    }

    @Override
    public List<TypeStatisticsVO> getTypeStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null) startTime = LocalDate.now().minusDays(30).atStartOfDay();
        if (endTime == null) endTime = LocalDateTime.now();

        Map<Integer, List<TransferApplication>> typeMap = new HashMap<>();
        typeMap.put(1, new ArrayList<>());
        typeMap.put(2, new ArrayList<>());
        typeMap.put(3, new ArrayList<>());

        LambdaQueryWrapper<TransferApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(TransferApplication::getSubmitTime, startTime, endTime);
        List<TransferApplication> all = applicationMapper.selectList(wrapper);
        for (TransferApplication app : all) {
            Integer type = app.getTransferType() != null ? app.getTransferType() : 1;
            typeMap.computeIfAbsent(type, k -> new ArrayList<>()).add(app);
        }

        List<TypeStatisticsVO> result = new ArrayList<>();
        long total = all.isEmpty() ? 1 : all.size();

        for (Map.Entry<Integer, List<TransferApplication>> entry : typeMap.entrySet()) {
            TypeStatisticsVO vo = new TypeStatisticsVO();
            vo.setTransferType(entry.getKey());
            vo.setTransferTypeName(entry.getKey() == 1 ? "异地转移接续" :
                    entry.getKey() == 2 ? "同城转移" : "跨机构转移");

            List<TransferApplication> list = entry.getValue();
            vo.setTotalCount((long) list.size());
            vo.setCompletedCount(countByStatus(list, COMPLETED_STATUS));
            vo.setBacklogCount(countByStatus(list, PROCESSING_STATUS));

            LambdaQueryWrapper<CollaborationTask> toWrapper = new LambdaQueryWrapper<>();
            toWrapper.in(CollaborationTask::getApplicationId,
                    list.stream().map(TransferApplication::getId).collect(Collectors.toList()))
                    .in(CollaborationTask::getTaskStatus, TIMEOUT_TASK_STATUS);
            vo.setTimeoutCount((long) taskMapper.selectCount(toWrapper).intValue());

            vo.setCompletionRate(pct(vo.getCompletedCount(), vo.getTotalCount() == 0 ? 1 : vo.getTotalCount()));
            vo.setProportion(pct(vo.getTotalCount(), total));

            List<TransferApplication> completed = list.stream()
                    .filter(a -> COMPLETED_STATUS.contains(a.getApplicationStatus())
                            && a.getSubmitTime() != null && a.getCompleteTime() != null)
                    .collect(Collectors.toList());
            if (!completed.isEmpty()) {
                double avgDays = completed.stream()
                        .mapToLong(a -> Duration.between(a.getSubmitTime(), a.getCompleteTime()).toMinutes())
                        .average().orElse(0.0) / 1440.0;
                vo.setAvgProcessingDays(BigDecimal.valueOf(avgDays).setScale(2, RoundingMode.HALF_UP));
            } else {
                vo.setAvgProcessingDays(BigDecimal.ZERO);
            }

            vo.setTotalTransferAmount(list.stream()
                    .filter(a -> a.getActualTransferAmount() != null)
                    .map(TransferApplication::getActualTransferAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            result.add(vo);
        }
        result.sort((a, b) -> Long.compare(b.getTotalCount(), a.getTotalCount()));
        return result;
    }

    @Override
    public List<RejectStatisticsVO> getRejectStatistics(LocalDateTime startTime, LocalDateTime endTime,
                                                         String regionCode) {
        if (startTime == null) startTime = LocalDate.now().minusDays(90).atStartOfDay();
        if (endTime == null) endTime = LocalDateTime.now();

        LambdaQueryWrapper<CollaborationTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CollaborationTask::getTaskStatus, TaskStatusEnum.REJECTED.getCode())
                .between(CollaborationTask::getConfirmTime, startTime, endTime)
                .isNotNull(CollaborationTask::getRejectReasonCode);
        if (StrUtil.isNotBlank(regionCode)) {
            wrapper.eq(CollaborationTask::getTargetRegion, regionCode);
        }
        List<CollaborationTask> rejectedTasks = taskMapper.selectList(wrapper);

        Map<String, List<CollaborationTask>> reasonMap = rejectedTasks.stream()
                .filter(t -> StrUtil.isNotBlank(t.getRejectReasonCode()))
                .collect(Collectors.groupingBy(CollaborationTask::getRejectReasonCode));

        long total = rejectedTasks.isEmpty() ? 1 : rejectedTasks.size();
        List<RejectStatisticsVO> result = new ArrayList<>();

        for (Map.Entry<String, List<CollaborationTask>> entry : reasonMap.entrySet()) {
            RejectStatisticsVO vo = new RejectStatisticsVO();
            vo.setRejectReasonCode(entry.getKey());
            List<CollaborationTask> list = entry.getValue();
            CollaborationTask first = list.get(0);
            vo.setRejectReasonName(first.getRejectReasonName());

            vo.setRejectCount((long) list.size());
            vo.setProportion(pct(vo.getRejectCount(), total));
            result.add(vo);
        }
        result.sort((a, b) -> Long.compare(b.getRejectCount(), a.getRejectCount()));
        return result;
    }

    @Override
    public BacklogAnalysisVO getBacklogAnalysis(String regionCode) {
        BacklogAnalysisVO vo = new BacklogAnalysisVO();

        LambdaQueryWrapper<TransferApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(TransferApplication::getApplicationStatus, PROCESSING_STATUS);
        if (StrUtil.isNotBlank(regionCode)) {
            wrapper.and(w -> w.eq(TransferApplication::getTransferOutRegion, regionCode)
                    .or()
                    .eq(TransferApplication::getTransferInRegion, regionCode));
        }
        List<TransferApplication> backlogApps = applicationMapper.selectList(wrapper);
        vo.setTotalBacklogCount((long) backlogApps.size());

        Map<String, Long> statusGroup = new LinkedHashMap<>();
        for (TransferApplication app : backlogApps) {
            ApplicationStatusEnum e = ApplicationStatusEnum.getByCode(app.getApplicationStatus());
            String name = e != null ? e.getName() : "未知";
            statusGroup.merge(name, 1L, Long::sum);
        }

        List<BacklogAnalysisVO.BacklogItemVO> items = new ArrayList<>();
        for (Map.Entry<String, Long> entry : statusGroup.entrySet()) {
            BacklogAnalysisVO.BacklogItemVO item = new BacklogAnalysisVO.BacklogItemVO();
            item.setStatusName(entry.getKey());
            item.setCount(entry.getValue());
            item.setProportion(vo.getTotalBacklogCount() == 0 ? 0.0 :
                    Math.round(entry.getValue() * 10000.0 / vo.getTotalBacklogCount()) / 100.0);
            items.add(item);
        }
        vo.setBacklogItems(items);

        vo.setOutRegionBacklog(backlogApps.stream()
                .filter(a -> a.getApplicationStatus() != null
                        && (a.getApplicationStatus().equals(30) || a.getApplicationStatus().equals(40))).count());
        vo.setInRegionBacklog(backlogApps.stream()
                .filter(a -> a.getApplicationStatus() != null
                        && (a.getApplicationStatus().equals(50) || a.getApplicationStatus().equals(60))).count());
        vo.setSupplementBacklog(backlogApps.stream()
                .filter(a -> a.getApplicationStatus() != null && a.getApplicationStatus().equals(70)).count());
        vo.setPendingReviewBacklog(backlogApps.stream()
                .filter(a -> a.getApplicationStatus() != null && a.getApplicationStatus() <= 20).count());

        return vo;
    }

    @Override
    public TimeoutAnalysisVO getTimeoutAnalysis(LocalDateTime startTime, LocalDateTime endTime,
                                                 String regionCode) {
        if (startTime == null) startTime = LocalDate.now().minusDays(30).atStartOfDay();
        if (endTime == null) endTime = LocalDateTime.now();

        TimeoutAnalysisVO vo = new TimeoutAnalysisVO();

        LambdaQueryWrapper<UrgeRecord> urgeWrapper = new LambdaQueryWrapper<>();
        urgeWrapper.between(UrgeRecord::getCreateTime, startTime, endTime);
        if (StrUtil.isNotBlank(regionCode)) {
            urgeWrapper.eq(UrgeRecord::getTargetRegion, regionCode);
        }
        List<UrgeRecord> urgeRecords = urgeRecordMapper.selectList(urgeWrapper);

        vo.setTotalTimeoutCount((long) urgeRecords.size());
        vo.setAutoUrgedCount(urgeRecords.stream().filter(u -> u.getUrgeType() != null && u.getUrgeType() == 1).count());
        vo.setManualUrgedCount(urgeRecords.stream().filter(u -> u.getUrgeType() != null && u.getUrgeType() == 2).count());

        Map<String, List<UrgeRecord>> taskGroup = urgeRecords.stream()
                .filter(u -> u.getTaskId() != null)
                .collect(Collectors.groupingBy(u -> u.getTaskId().toString()));
        if (!taskGroup.isEmpty()) {
            double avg = taskGroup.values().stream().mapToLong(List::size).average().orElse(0.0);
            vo.setAvgUrgeCountPerTask(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        } else {
            vo.setAvgUrgeCountPerTask(BigDecimal.ZERO);
        }

        long total = urgeRecords.isEmpty() ? 1 : urgeRecords.size();
        Map<String, List<UrgeRecord>> regionGroup = urgeRecords.stream()
                .filter(u -> StrUtil.isNotBlank(u.getTargetRegion()))
                .collect(Collectors.groupingBy(UrgeRecord::getTargetRegion));
        List<TimeoutAnalysisVO.TimeoutDistributionVO> byRegion = new ArrayList<>();
        for (Map.Entry<String, List<UrgeRecord>> entry : regionGroup.entrySet()) {
            TimeoutAnalysisVO.TimeoutDistributionVO d = new TimeoutAnalysisVO.TimeoutDistributionVO();
            d.setDimension(entry.getKey());
            d.setDimensionName(getRegionName(entry.getKey()));
            d.setCount((long) entry.getValue().size());
            d.setProportion(pct(d.getCount(), total));
            byRegion.add(d);
        }
        byRegion.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));
        vo.setByRegion(byRegion);

        Map<Integer, List<UrgeRecord>> levelGroup = urgeRecords.stream()
                .filter(u -> u.getUrgeLevel() != null)
                .collect(Collectors.groupingBy(UrgeRecord::getUrgeLevel));
        List<TimeoutAnalysisVO.TimeoutDistributionVO> byLevel = new ArrayList<>();
        for (Map.Entry<Integer, List<UrgeRecord>> entry : levelGroup.entrySet()) {
            TimeoutAnalysisVO.TimeoutDistributionVO d = new TimeoutAnalysisVO.TimeoutDistributionVO();
            d.setDimension(entry.getKey().toString());
            d.setDimensionName(entry.getKey() == 1 ? "一般提醒" : entry.getKey() == 2 ? "重要提醒" : "加急");
            d.setCount((long) entry.getValue().size());
            d.setProportion(pct(d.getCount(), total));
            byLevel.add(d);
        }
        vo.setByLevel(byLevel);

        List<TimeoutAnalysisVO.TimeoutDistributionVO> byTaskType = new ArrayList<>();
        for (int taskType : new int[]{1, 2, 3, 4}) {
            String finalRegion = regionCode;
            LambdaQueryWrapper<CollaborationTask> ttWrapper = new LambdaQueryWrapper<>();
            ttWrapper.eq(CollaborationTask::getTaskType, taskType)
                    .in(CollaborationTask::getTaskStatus, TIMEOUT_TASK_STATUS)
                    .between(CollaborationTask::getLastUrgeTime, startTime, endTime);
            if (StrUtil.isNotBlank(finalRegion)) {
                ttWrapper.eq(CollaborationTask::getTargetRegion, finalRegion);
            }
            long count = taskMapper.selectCount(ttWrapper);
            if (count > 0) {
                TimeoutAnalysisVO.TimeoutDistributionVO d = new TimeoutAnalysisVO.TimeoutDistributionVO();
                d.setDimension(String.valueOf(taskType));
                d.setDimensionName(taskType == 1 ? "转出确认" : taskType == 2 ? "转入确认"
                        : taskType == 3 ? "信息补正" : "退件复核");
                d.setCount(count);
                d.setProportion(pct(count, total));
                byTaskType.add(d);
            }
        }
        vo.setByTaskType(byTaskType);

        return vo;
    }

    @Override
    public ChannelStatisticsVO getChannelStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null) startTime = LocalDate.now().minusDays(30).atStartOfDay();
        if (endTime == null) endTime = LocalDateTime.now();

        LambdaQueryWrapper<TransferApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(TransferApplication::getSubmitTime, startTime, endTime);
        List<TransferApplication> all = applicationMapper.selectList(wrapper);

        Map<Integer, List<TransferApplication>> channelGroup = all.stream()
                .filter(a -> a.getChannelType() != null)
                .collect(Collectors.groupingBy(TransferApplication::getChannelType));

        long total = all.isEmpty() ? 1 : all.size();
        List<ChannelStatisticsVO.ChannelItemVO> channels = new ArrayList<>();

        for (Map.Entry<Integer, List<TransferApplication>> entry : channelGroup.entrySet()) {
            ChannelStatisticsVO.ChannelItemVO item = new ChannelStatisticsVO.ChannelItemVO();
            item.setChannelType(entry.getKey());
            ChannelTypeEnum ce = ChannelTypeEnum.getByCode(entry.getKey());
            item.setChannelTypeName(ce != null ? ce.getName() : "未知渠道");

            List<TransferApplication> list = entry.getValue();
            item.setTotalCount((long) list.size());
            item.setCompletedCount(countByStatus(list, COMPLETED_STATUS));
            item.setCompletionRate(pct(item.getCompletedCount(), item.getTotalCount() == 0 ? 1 : item.getTotalCount()));
            item.setProportion(pct(item.getTotalCount(), total));
            channels.add(item);
        }
        channels.sort((a, b) -> Long.compare(b.getTotalCount(), a.getTotalCount()));

        ChannelStatisticsVO vo = new ChannelStatisticsVO();
        vo.setChannels(channels);
        return vo;
    }

    @Override
    public EfficiencyAnalysisVO getEfficiencyAnalysis(LocalDateTime startTime, LocalDateTime endTime,
                                                        String regionCode) {
        if (startTime == null) startTime = LocalDate.now().minusDays(90).atStartOfDay();
        if (endTime == null) endTime = LocalDateTime.now();

        EfficiencyAnalysisVO vo = new EfficiencyAnalysisVO();
        if (StrUtil.isNotBlank(regionCode)) {
            vo.setRegionCode(regionCode);
            vo.setRegionName(getRegionName(regionCode));
        }

        LambdaQueryWrapper<TransferApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(TransferApplication::getCompleteTime, startTime, endTime)
                .in(TransferApplication::getApplicationStatus, COMPLETED_STATUS)
                .isNotNull(TransferApplication::getSubmitTime)
                .isNotNull(TransferApplication::getAuditTime)
                .isNotNull(TransferApplication::getTransferOutTime)
                .isNotNull(TransferApplication::getTransferInTime);
        if (StrUtil.isNotBlank(regionCode)) {
            wrapper.and(w -> w.eq(TransferApplication::getTransferOutRegion, regionCode)
                    .or()
                    .eq(TransferApplication::getTransferInRegion, regionCode));
        }
        List<TransferApplication> apps = applicationMapper.selectList(wrapper);

        if (!apps.isEmpty()) {
            vo.setAvgTotalDays(avgDuration(apps,
                    a -> a.getSubmitTime(), a -> a.getCompleteTime()));
            vo.setAvgRuleAuditDays(avgDuration(apps,
                    a -> a.getSubmitTime(), a -> a.getAuditTime()));
            vo.setAvgTransferOutDays(avgDuration(apps,
                    a -> a.getAuditTime(), a -> a.getTransferOutTime()));
            vo.setAvgTransferInDays(avgDuration(apps,
                    a -> a.getTransferOutTime(), a -> a.getTransferInTime()));
        } else {
            vo.setAvgTotalDays(BigDecimal.ZERO);
            vo.setAvgRuleAuditDays(BigDecimal.ZERO);
            vo.setAvgTransferOutDays(BigDecimal.ZERO);
            vo.setAvgTransferInDays(BigDecimal.ZERO);
            vo.setAvgSupplementDays(BigDecimal.ZERO);
        }

        vo.setSLAComplianceRate(pct(apps.stream()
                .filter(a -> a.getSubmitTime() != null && a.getCompleteTime() != null
                        && ChronoUnit.DAYS.between(a.getSubmitTime().toLocalDate(),
                        a.getCompleteTime().toLocalDate()) <= 15)
                .count(), apps.isEmpty() ? 1 : (long) apps.size()));

        LambdaQueryWrapper<TransferApplication> effWrapper = new LambdaQueryWrapper<>();
        effWrapper.between(TransferApplication::getCompleteTime, startTime, endTime)
                .in(TransferApplication::getApplicationStatus, COMPLETED_STATUS);
        if (StrUtil.isNotBlank(regionCode)) {
            effWrapper.and(w -> w.eq(TransferApplication::getTransferOutRegion, regionCode)
                    .or()
                    .eq(TransferApplication::getTransferInRegion, regionCode));
        }
        List<TransferApplication> effApps = applicationMapper.selectList(effWrapper);

        vo.setWithin3Days(0L);
        vo.setWithin7Days(0L);
        vo.setWithin15Days(0L);
        vo.setOver15Days(0L);
        for (TransferApplication a : effApps) {
            if (a.getSubmitTime() == null || a.getCompleteTime() == null) continue;
            long days = ChronoUnit.DAYS.between(a.getSubmitTime().toLocalDate(), a.getCompleteTime().toLocalDate());
            if (days <= 3) vo.setWithin3Days(vo.getWithin3Days() + 1);
            else if (days <= 7) vo.setWithin7Days(vo.getWithin7Days() + 1);
            else if (days <= 15) vo.setWithin15Days(vo.getWithin15Days() + 1);
            else vo.setOver15Days(vo.getOver15Days() + 1);
        }

        return vo;
    }

    private long countByStatus(List<TransferApplication> list, List<Integer> statusList) {
        return list.stream()
                .filter(a -> a.getApplicationStatus() != null
                        && statusList.contains(a.getApplicationStatus()))
                .count();
    }

    private BigDecimal pct(long part, long total) {
        if (total == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(part * 10000.0 / total)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal avgDuration(List<TransferApplication> apps,
                                   java.util.function.Function<TransferApplication, LocalDateTime> start,
                                   java.util.function.Function<TransferApplication, LocalDateTime> end) {
        double avg = apps.stream()
                .mapToLong(a -> {
                    LocalDateTime s = start.apply(a);
                    LocalDateTime e = end.apply(a);
                    return s != null && e != null ? Duration.between(s, e).toMinutes() : 0L;
                })
                .filter(d -> d > 0)
                .average().orElse(0.0) / 1440.0;
        return BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
    }

    private List<LocalDateTime[]> generatePeriods(LocalDateTime start, LocalDateTime end, String granularity) {
        List<LocalDateTime[]> periods = new ArrayList<>();
        LocalDateTime current = start;
        while (current.isBefore(end)) {
            LocalDateTime next;
            switch (granularity.toUpperCase()) {
                case "HOUR":
                    next = current.plusHours(1);
                    break;
                case "WEEK":
                    next = current.plusWeeks(1);
                    break;
                case "MONTH":
                    next = current.plusMonths(1);
                    break;
                case "QUARTER":
                    next = current.plusMonths(3);
                    break;
                default:
                    next = current.plusDays(1);
            }
            if (next.isAfter(end)) next = end;
            periods.add(new LocalDateTime[]{current, next});
            current = next;
        }
        return periods;
    }

    private DateTimeFormatter getFormatter(String granularity) {
        switch (granularity.toUpperCase()) {
            case "HOUR": return DateTimeFormatter.ofPattern("MM-dd HH");
            case "WEEK": return DateTimeFormatter.ofPattern("yyyy-'W'ww");
            case "MONTH": return DateTimeFormatter.ofPattern("yyyy-MM");
            case "QUARTER": return DateTimeFormatter.ofPattern("yyyy-'Q'q");
            default: return DateTimeFormatter.ofPattern("MM-dd");
        }
    }

    private List<KpiTrendVO> buildKpiTrends(LocalDateTime start, LocalDateTime end) {
        List<KpiTrendVO> trends = new ArrayList<>();
        List<LocalDateTime[]> periods = generatePeriods(start, end, "DAY");
        for (LocalDateTime[] period : periods) {
            KpiTrendVO vo = new KpiTrendVO();
            vo.setDate(period[0].format(DateTimeFormatter.ofPattern("MM-dd")));

            LambdaQueryWrapper<TransferApplication> applyWrapper = new LambdaQueryWrapper<>();
            applyWrapper.between(TransferApplication::getSubmitTime, period[0], period[1]);
            vo.setApplyCount((long) applicationMapper.selectCount(applyWrapper).intValue());

            LambdaQueryWrapper<TransferApplication> completeWrapper = new LambdaQueryWrapper<>();
            completeWrapper.between(TransferApplication::getCompleteTime, period[0], period[1])
                    .in(TransferApplication::getApplicationStatus, COMPLETED_STATUS);
            vo.setCompletedCount((long) applicationMapper.selectCount(completeWrapper).intValue());
            vo.setCompletionRate(pct(vo.getCompletedCount(), vo.getApplyCount() == 0 ? 1 : vo.getApplyCount()));
            trends.add(vo);
        }
        return trends;
    }

    private RegionInfo getRegionByCode(String code) {
        LambdaQueryWrapper<RegionInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RegionInfo::getRegionCode, code).last("LIMIT 1");
        return regionInfoMapper.selectOne(wrapper);
    }

    private String getRegionName(String code) {
        if (StrUtil.isBlank(code)) return "";
        RegionInfo r = getRegionByCode(code);
        return r != null ? r.getRegionName() : code;
    }
}
