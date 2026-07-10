package com.smartelderly.api.ai.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.smartelderly.api.ai.dto.AiNotifyFamilyResponse;
import com.smartelderly.api.ai.dto.RecommendedDepartmentDTO;

@Service
public class AiFamilyNotificationService {

    public AiNotifyFamilyResponse notifyFamily(Long consultationId, Long elderlyId, String inputText, String finalAnswer,
            String riskLevel, List<RecommendedDepartmentDTO> departments, Boolean needMedicalVisit, boolean hasFamilyBinding) {
        if (!hasFamilyBinding) {
            return AiNotifyFamilyResponse.builder()
                    .consultationId(consultationId)
                    .success(Boolean.FALSE)
                    .message("当前老人暂未绑定家属，无法同步通知")
                    .notifyChannel("none")
                    .hasFamilyBinding(Boolean.FALSE)
                    .build();
        }
        return AiNotifyFamilyResponse.builder()
                .consultationId(consultationId)
                .success(Boolean.TRUE)
                .message("家属同步已保存")
                .notifyChannel("record_only")
                .hasFamilyBinding(Boolean.TRUE)
                .build();
    }
}
