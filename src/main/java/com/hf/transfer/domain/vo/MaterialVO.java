package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class MaterialVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String materialType;
    private String materialTypeName;
    private String materialName;
    private String fileId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileFormat;
    private Integer verifyStatus;
    private String verifyStatusName;
    private String verifyRemark;
    private Integer materialRound;
}
