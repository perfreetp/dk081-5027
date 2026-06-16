package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ChannelStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<ChannelItemVO> channels;

    @Data
    public static class ChannelItemVO implements Serializable {
        private Integer channelType;
        private String channelTypeName;
        private Long totalCount;
        private Long completedCount;
        private BigDecimal completionRate;
        private BigDecimal proportion;
    }
}
