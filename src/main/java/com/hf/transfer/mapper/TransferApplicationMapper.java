package com.hf.transfer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hf.transfer.domain.entity.TransferApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface TransferApplicationMapper extends BaseMapper<TransferApplication> {

    List<Map<String, Object>> selectRegionSummary(@Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime,
                                                   @Param("transferType") Integer transferType);

    List<Map<String, Object>> selectTimeSummary(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime,
                                                 @Param("granularity") String granularity);

    Map<String, Object> selectStatisticsOverview(@Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);
}
