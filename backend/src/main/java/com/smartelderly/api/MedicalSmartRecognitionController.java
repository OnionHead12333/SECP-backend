package com.smartelderly.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smartelderly.api.dto.ApiResponse;
import com.smartelderly.api.dto.medical.MedicalSmartRecognitionResultDto;
import com.smartelderly.api.medical.MedicalMultipartValidator;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.medical.MedicalSmartRecognitionService;

@RestController
@RequestMapping("/v1/medical")
public class MedicalSmartRecognitionController {

    private final MedicalSmartRecognitionService medicalSmartRecognitionService;

    public MedicalSmartRecognitionController(MedicalSmartRecognitionService medicalSmartRecognitionService) {
        this.medicalSmartRecognitionService = medicalSmartRecognitionService;
    }

    @PostMapping(value = "/smart-recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MedicalSmartRecognitionResultDto> smartRecognize(
            @RequestParam(value = "elderProfileId", required = false) Long elderProfileId,
            @RequestParam("file") MultipartFile file) {
        MedicalMultipartValidator.validate(file);
        var principal = SecurityUtils.requireAuth();
        return ApiResponse.success(
                medicalSmartRecognitionService.recognizeAndArchive(principal, elderProfileId, file));
    }
}
