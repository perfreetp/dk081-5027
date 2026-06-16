package com.hf.transfer.controller;

import com.hf.transfer.common.R;
import com.hf.transfer.domain.vo.*;
import com.hf.transfer.service.StatisticsSuperviseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "5. 统计监管模块", description = "内部监管统计：积压/超时/退件率/效率/渠道等多维度分析")
@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsSuperviseController {

    private final StatisticsSuperviseService statisticsSuperviseService;

    @Operation(summary = "统计总览", description = "核心KPI：申请量/办结量/积压/超时/退件率/补正率/平均耗时/总金额/完成率趋势")
    @GetMapping("/overview")
    public R<StatisticsOverviewVO> getOverview(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.success(statisticsSuperviseService.getOverview(startTime, endTime));
    }

    @Operation(summary = "按地区统计分析", description = "转出/转入维度排名（A/B/C三级），完成率/超时率/平均处理天数")
    @GetMapping("/region")
    public R<List<RegionStatisticsVO>> getRegionStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "指定地区编码") @RequestParam(required = false) String regionCode,
            @Parameter(description = "角色维度：0全部 1转出 2转入") @RequestParam(defaultValue = "0") Integer roleType) {
        return R.success(statisticsSuperviseService.getRegionStatistics(startTime, endTime, regionCode, roleType));
    }

    @Operation(summary = "按时间维度统计趋势", description = "支持小时/日/周/月/季粒度")
    @GetMapping("/time")
    public R<List<TimeStatisticsVO>> getTimeStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "时间粒度：HOUR DAY WEEK MONTH QUARTER") @RequestParam(defaultValue = "DAY") String granularity) {
        return R.success(statisticsSuperviseService.getTimeStatistics(startTime, endTime, granularity));
    }

    @Operation(summary = "按业务类型统计", description = "异地/同城/跨机构转移占比与效率对比")
    @GetMapping("/type")
    public R<List<TypeStatisticsVO>> getTypeStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.success(statisticsSuperviseService.getTypeStatistics(startTime, endTime));
    }

    @Operation(summary = "退件原因统计分析", description = "Top退件原因及可补正率")
    @GetMapping("/reject")
    public R<List<RejectStatisticsVO>> getRejectStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) String regionCode) {
        return R.success(statisticsSuperviseService.getRejectStatistics(startTime, endTime, regionCode));
    }

    @Operation(summary = "积压分析", description = "当前在办积压按状态维度分布")
    @GetMapping("/backlog")
    public R<BacklogAnalysisVO> getBacklogAnalysis(
            @RequestParam(required = false) String regionCode) {
        return R.success(statisticsSuperviseService.getBacklogAnalysis(regionCode));
    }

    @Operation(summary = "超时分析", description = "催办次数/等级/地区分布")
    @GetMapping("/timeout")
    public R<TimeoutAnalysisVO> getTimeoutAnalysis(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) String regionCode) {
        return R.success(statisticsSuperviseService.getTimeoutAnalysis(startTime, endTime, regionCode));
    }

    @Operation(summary = "渠道统计", description = "8种申请渠道的申请量占比与完成率")
    @GetMapping("/channel")
    public R<ChannelStatisticsVO> getChannelStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.success(statisticsSuperviseService.getChannelStatistics(startTime, endTime));
    }

    @Operation(summary = "效率分析", description = "5阶段平均耗时 + SLA达标率（3/7/15天分档）")
    @GetMapping("/efficiency")
    public R<EfficiencyAnalysisVO> getEfficiencyAnalysis(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) String regionCode) {
        return R.success(statisticsSuperviseService.getEfficiencyAnalysis(startTime, endTime, regionCode));
    }
}
