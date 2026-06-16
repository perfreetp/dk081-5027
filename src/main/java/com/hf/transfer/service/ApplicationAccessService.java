package com.hf.transfer.service;

import com.hf.transfer.common.PageResult;
import com.hf.transfer.domain.dto.ApplicationQueryDTO;
import com.hf.transfer.domain.dto.TransferApplyDTO;
import com.hf.transfer.domain.vo.ApplicationDetailVO;
import com.hf.transfer.domain.vo.ApplicationListVO;
import com.hf.transfer.domain.vo.ProgressQueryVO;

public interface ApplicationAccessService {

    String submitApplication(TransferApplyDTO dto, String operatorId, String operatorName);

    ProgressQueryVO queryProgress(String applicationNo, String idCardNo);

    ApplicationDetailVO getApplicationDetail(Long id);

    PageResult<ApplicationListVO> queryApplicationPage(ApplicationQueryDTO dto);

    void cancelApplication(Long id, String operatorId, String operatorName, String reason);
}
