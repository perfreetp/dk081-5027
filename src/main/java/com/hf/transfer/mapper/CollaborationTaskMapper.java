package com.hf.transfer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hf.transfer.domain.entity.CollaborationTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface CollaborationTaskMapper extends BaseMapper<CollaborationTask> {

    List<CollaborationTask> selectTimeoutTasks(@Param("deadlineTime") LocalDateTime deadlineTime,
                                                @Param("statusList") List<Integer> statusList);

    List<Map<String, Object>> selectRegionTaskStatistics(@Param("startTime") LocalDateTime startTime,
                                                          @Param("endTime") LocalDateTime endTime);
}
