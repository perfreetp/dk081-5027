package com.hf.transfer.service;

import com.hf.transfer.domain.vo.*;

import java.time.LocalDateTime;
import java.util.List;

public interface StatisticsSuperviseService {

    StatisticsOverviewVO getOverview(LocalDateTime startTime, LocalDateTime endTime);

    List<RegionStatisticsVO> getRegionStatistics(LocalDateTime startTime, LocalDateTime endTime,
                                                   String regionCode, Integer roleType);

    List<TimeStatisticsVO> getTimeStatistics(LocalDateTime startTime, LocalDateTime endTime,
                                              String granularity);

    List<TypeStatisticsVO> getTypeStatistics(LocalDateTime startTime, LocalDateTime endTime);

    List<RejectStatisticsVO> getRejectStatistics(LocalDateTime startTime, LocalDateTime endTime,
                                                  String regionCode);

    BacklogAnalysisVO getBacklogAnalysis(String regionCode);

    TimeoutAnalysisVO getTimeoutAnalysis(LocalDateTime startTime, LocalDateTime endTime,
                                          String regionCode);

    ChannelStatisticsVO getChannelStatistics(LocalDateTime startTime, LocalDateTime endTime);

    EfficiencyAnalysisVO getEfficiencyAnalysis(LocalDateTime startTime, LocalDateTime endTime,
                                                String regionCode);

    TodoDashboardVO getTodoDashboard(String regionCode);
}
