package com.hf.transfer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hf.transfer.domain.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
