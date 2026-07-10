package com.smartelderly.service.ocr;

/**
 * 百度云医疗票据「结构化识别」接口（与 {@link MedicalDocClassRouter} 配套）。
 * path 为 {@code https://aip.baidubce.com} 之后的 URI path。
 */
public enum SpecializedMedicalOcrApi {

    MEDICAL_PRESCRIPTION("medical_prescription", "/rest/2.0/ocr/v1/medical_prescription"),
    MEDICAL_INVOICE("medical_invoice", "/rest/2.0/ocr/v1/medical_invoice"),
    MEDICAL_DETAIL("medical_detail", "/rest/2.0/ocr/v1/medical_detail"),
    MEDICAL_STATEMENT("medical_statement", "/rest/2.0/ocr/v1/medical_statement"),
    MEDICAL_REPORT_DETECTION("medical_report_detection", "/rest/2.0/ocr/v1/medical_report_detection"),
    MEDICAL_SUMMARY("medical_summary", "/rest/2.0/ocr/v1/medical_summary"),
    HEALTH_REPORT("health_report", "/rest/2.0/ocr/v1/health_report"),
    MEDICAL_RECORD("medical_record", "/rest/2.0/ocr/v1/medical_record"),
    MEDICAL_OUTPATIENT("medical_outpatient", "/rest/2.0/ocr/v1/medical_outpatient"),
    MEDICAL_SURGERY("medical_surgery", "/rest/2.0/ocr/v1/medical_surgery"),
    MEDICAL_SUMMARY_IN_HOSPITAL(
            "medical_summary_in_hospital", "/rest/2.0/ocr/v1/medical_summary_in_hospital");

    private final String apiId;
    private final String path;

    SpecializedMedicalOcrApi(String apiId, String path) {
        this.apiId = apiId;
        this.path = path;
    }

    public String getApiId() {
        return apiId;
    }

    public String getPath() {
        return path;
    }
}
