package com.hf.transfer.controller;

import com.hf.transfer.common.R;
import com.hf.transfer.domain.vo.*;
import com.hf.transfer.service.StatisticsSuperviseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Api(tags = "5. 统计监管模块 - 内部监管统计与汇总分析")
@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsSuperviseController {

    private final StatisticsSuperviseService statisticsSuperviseService;

    @ApiOperation("5.1 统计总览 - 积压/超时/退件率等核心KPI")
    @GetMapping("/overview")
    public R<StatisticsOverviewVO> getOverview(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.success(statisticsSuperviseService.getOverview(startTime, endTime));
    }

    @ApiOperation("5.2 按地区统计分析")
    @GetMapping("/region")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "regionCode", value = "指定地区编码（不填则全部）"),
            @ApiImplicitParam(name = "roleType", value = "角色维度：0全部 1转出 2转入")
    })
    public R<List<RegionStatisticsVO>> getRegionStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) String regionCode,
            @RequestParam(defaultValue = "0") Integer roleType) {
        return R.success(statisticsSuperviseService.getRegionStatistics(
                startTime, endTime, regionCode, roleType));
    }

    @ApiOperation("5.3 按时间维度统计趋势")
    @GetMapping("/time")
    @ApiImplicitParam(name = "granularity", value = "时间粒度：HOUR小时 DAY日 WEEK周 MONTH月 QUARTER季", defaultValue = "DAY")
    public R<List<TimeStatisticsVO>> getTimeStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "DAY") String granularity) {
        return R.success(statisticsSuperviseService.getTimeStatistics(startTime, endTime, granularity));
    }

    @ApiOperation("5.4 按业务类型统计")
    @GetMapping("/type")
    public R<List<TypeStatisticsVO>> getTypeStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.success(statisticsSuperviseService.getTypeStatistics(startTime, endTime));
    }

    @ApiOperation("5.5 退件原因统计分析")
    @GetMapping("/reject")
    @ApiImplicitParam(name = "regionCode", value = "指定地区编码（可选）")
    public R<List<RejectStatisticsVO>> getRejectStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) String regionCode) {
        return R.success(statisticsSuperviseService.getRejectStatistics(startTime, endTime, regionCode));
    }

    @ApiOperation("5.6 积压分析 - 当前在办积压分布")
    @GetMapping("/backlog")
    @ApiImplicitParam(name = "regionCode", value = "指定地区编码（可选）")
    public R<BacklogAnalysisVO> getBacklogAnalysis(@RequestParam(required = false) String regionCode) {
        return R.success(statisticsSuperviseService.getBacklogAnalysis(regionCode));
    }

    @ApiOperation("5.7 超时分析 - 催办次数/等级/地区分布")
    @GetMapping("/timeout")
    @ApiImplicitParam(name = "regionCode", value = "指定地区编码（可选）")
    public R<TimeoutAnalysisVO> getTimeoutAnalysis(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) String regionCode) {
        return R.success(statisticsSuperviseService.getTimeoutAnalysis(startTime, endTime, regionCode));
    }

    @ApiOperation("5.8 渠道统计 - 各申请渠道分布")
    @GetMapping("/channel")
    public R<ChannelStatisticsVO> getChannelStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.success(statisticsSuperviseService.getChannelStatistics(startTime, endTime));
    }

    @ApiOperation("5.9 效率分析 - 各阶段平均耗时与SLA达标率")
    @GetMapping("/efficiency")
    @ApiImplicitParam(name = "regionCode", value = "指定地区编码（可选）")
    public R<EfficiencyAnalysisVO> getEfficiencyAnalysis(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) String regionCode) {
        return R.success(statisticsSuperviseService.getEfficiencyAnalysis(startTime, endTime, regionCode));
    }
}
