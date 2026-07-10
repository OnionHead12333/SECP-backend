package com.smartelderly.api.ai.service;

import org.springframework.stereotype.Service;

import com.smartelderly.api.ai.dto.AiFeedbackRequest;
import com.smartelderly.api.ai.dto.AiFeedbackResponse;

@Service
public class AiFeedbackService {

    public AiFeedbackResponse saveFeedback(Long consultationId, AiFeedbackRequest request) {
        return AiFeedbackResponse.builder()
                .consultationId(consultationId)
                .success(Boolean.TRUE)
                .message("反馈已提交")
                .build();
    }
}
