package com.smartelderly.service.ocr;

import java.util.Optional;

/**
 * 将百度云「文件检测分类」输出的 {@code type} 映射到结构化医疗 OCR 接口。
 * <p>
 * 分类字符串来自官方附录（如 {@code 医疗_门诊处方笺}），采用包含匹配；先匹配的优先级更高。
 */
public final class MedicalDocClassRouter {

    private MedicalDocClassRouter() {}

    /** @param docClassifyType 来自 doc_classify 的 {@code words_result[].type} */
    public static Optional<SpecializedMedicalOcrApi> resolve(String docClassifyType) {
        if (docClassifyType == null || docClassifyType.isBlank()) {
            return Optional.empty();
        }
        final String t = docClassifyType.trim();

        if (t.contains("处方") || t.contains("医嘱")) {
            return Optional.of(SpecializedMedicalOcrApi.MEDICAL_PRESCRIPTION);
        }
        if (t.contains("病案首页")) {
            return Optional.of(SpecializedMedicalOcrApi.MEDICAL_RECORD);
        }
        if (t.contains("门诊病历")) {
            return Optional.of(SpecializedMedicalOcrApi.MEDICAL_OUTPATIENT);
        }
        if (t.contains("费用明细")) {
            return Optional.of(SpecializedMedicalOcrApi.MEDICAL_DETAIL);
        }
        if (t.contains("结算单")) {
            return Optional.of(SpecializedMedicalOcrApi.MEDICAL_STATEMENT);
        }
        if (t.startsWith("医疗_") && t.contains("发票")) {
            return Optional.of(SpecializedMedicalOcrApi.MEDICAL_INVOICE);
        }

        if (t.contains("入院记录") || t.contains("入院小结")) {
            return Optional.of(SpecializedMedicalOcrApi.MEDICAL_SUMMARY_IN_HOSPITAL);
        }

        if (t.contains("出院小结")
                || t.contains("出院记录")
                || t.contains("出院诊断证明")
                || t.contains("出院通知")
                || t.equals("医疗出院证")) {
            return Optional.of(SpecializedMedicalOcrApi.MEDICAL_SUMMARY);
        }

        if (t.contains("手术记录")) {
            return Optional.of(SpecializedMedicalOcrApi.MEDICAL_SURGERY);
        }

        if (isLabOrImagingReport(t)) {
            return Optional.of(SpecializedMedicalOcrApi.MEDICAL_REPORT_DETECTION);
        }

        if (t.contains("诊断报告")) {
            return Optional.of(SpecializedMedicalOcrApi.HEALTH_REPORT);
        }

        return Optional.empty();
    }

    private static boolean isLabOrImagingReport(String t) {
        return t.contains("检验报告")
                || t.contains("检查报告")
                || t.contains("化验")
                || t.contains("血常规")
                || t.contains("尿常规")
                || t.contains("大便常规")
                || t.contains("血生化")
                || t.contains("血凝")
                || t.contains("乙肝五项")
                || t.contains("心电图")
                || t.contains("脑电图")
                || t.contains("超声检查")
                || t.contains("内窥镜检查")
                || t.contains("CT检查")
                || t.contains("MRI检查")
                || t.contains("X线检查")
                || t.contains("PET-CT检查")
                || t.contains("病理报告")
                || t.contains("视力检查")
                || t.contains("听力检查")
                || t.contains("肿瘤标志物")
                || t.contains("骨髓穿刺")
                || t.contains("羊水穿刺")
                || t.contains("介入检查")
                || t.contains("其他化验检查")
                || t.contains("其他检查");
    }
}
