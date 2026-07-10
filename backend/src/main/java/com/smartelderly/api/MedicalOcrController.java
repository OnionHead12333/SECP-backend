package com.smartelderly.api;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smartelderly.api.dto.ApiResponse;
import com.smartelderly.api.dto.MedicalOcrView;
import com.smartelderly.service.ocr.MedicalDocumentAnalysisService;

/**
 * 老人端 / 子女端共用：上传单据照片，服务端代理调用百度云 OCR。
 */
@RestController
@RequestMapping("/v1/ocr")
public class MedicalOcrController {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/bmp",
            "image/jpg");

    private final MedicalDocumentAnalysisService medicalDocumentAnalysisService;

    public MedicalOcrController(MedicalDocumentAnalysisService medicalDocumentAnalysisService) {
        this.medicalDocumentAnalysisService = medicalDocumentAnalysisService;
    }

    @PostMapping(value = "/medical-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MedicalOcrView> recognizeMedicalDocument(@RequestParam("file") MultipartFile file)
            throws IOException {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(4001, "请上传图片文件");
        }
        String ct = file.getContentType();
        if (ct == null || ALLOWED_TYPES.stream().noneMatch(t -> t.equalsIgnoreCase(ct.trim()))) {
            return ApiResponse.error(
                    4001,
                    "仅支持 jpg / jpeg / png / bmp，当前 Content-Type：" + (ct == null ? "未知" : ct));
        }
        String original = file.getOriginalFilename();
        if (original != null && !hasAllowedExtension(original)) {
            return ApiResponse.error(4001, "文件名后缀应为 .jpg .jpeg .png .bmp");
        }
        MedicalOcrView view = medicalDocumentAnalysisService.analyze(file.getBytes());
        return ApiResponse.success(view);
    }

    private static boolean hasAllowedExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return true;
        }
        String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("bmp");
    }
}
