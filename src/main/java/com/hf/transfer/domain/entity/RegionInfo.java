package com.hf.transfer.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("hf_region_info")
public class RegionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String regionCode;

    private String regionName;

    private String centerName;

    private String centerShort;

    private String provinceCode;

    private String provinceName;

    private String cityCode;

    private String cityName;

    private String contactPerson;

    private String contactPhone;

    private String contactEmail;

    private String apiEndpoint;

    private String apiSecret;

    private Integer status;

    private Integer sortOrder;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
