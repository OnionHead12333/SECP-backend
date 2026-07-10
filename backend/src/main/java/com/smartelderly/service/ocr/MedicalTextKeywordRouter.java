package com.smartelderly.service.ocr;

import java.util.List;
import java.util.Optional;

/**
 * 根据高精度 OCR 全文中的典型单据标题用语，路由到结构化医疗接口。
 * <p>
 * 优先级高于 {@link MedicalDocClassRouter}（云端分类易误判时，以字迹标题为准）。
 */
public final class MedicalTextKeywordRouter {

    private MedicalTextKeywordRouter() {}

    private record KeywordRule(SpecializedMedicalOcrApi api, String... phrases) {}

    /**
     * 规则自上而下匹配：同一 {@link SpecializedMedicalOcrApi} 内短语按数组顺序，越长、越具体的短语应靠前。
     */
    private static final List<KeywordRule> RULES =
            List.of(
                    new KeywordRule(
                            SpecializedMedicalOcrApi.MEDICAL_REPORT_DETECTION,
                            "检验报告单",
                            "医学检验报告单",
                            "临床检验报告单",
                            "检验结果报告单",
                            "实验室检验报告",
                            "化验报告单",
                            "检验报告",
                            "检查报告单",
                            "影像诊断报告单",
                            "检查报告",
                            "病理报告单",
                            "病理诊断报告",
                            "超声检查报告",
                            "心电图报告",
                            "CT诊断报告",
                            "MRI诊断报告",
                            "核磁共振诊断报告",
                            "X线诊断报告",
                            "DR诊断报告",
                            "肺功能检查报告"),
                    new KeywordRule(
                            SpecializedMedicalOcrApi.MEDICAL_RECORD, "住院病案首页", "病案首页"),
                    new KeywordRule(
                            SpecializedMedicalOcrApi.MEDICAL_OUTPATIENT,
                            "门急诊病历",
                            "门诊病历",
                            "急诊病历"),
                    new KeywordRule(
                            SpecializedMedicalOcrApi.MEDICAL_PRESCRIPTION,
                            "电子处方",
                            "处方笺",
                            "处方单",
                            "取药单",
                            "用药指导单"),
                    new KeywordRule(
                            SpecializedMedicalOcrApi.MEDICAL_SUMMARY_IN_HOSPITAL,
                            "入院记录",
                            "入院小结"),
                    new KeywordRule(
                            SpecializedMedicalOcrApi.MEDICAL_SUMMARY,
                            "出院小结",
                            "出院记录",
                            "出院诊断证明"),
                    new KeywordRule(
                            SpecializedMedicalOcrApi.MEDICAL_SURGERY, "手术记录", "麻醉记录单"),
                    new KeywordRule(
                            SpecializedMedicalOcrApi.MEDICAL_DETAIL,
                            "住院费用明细清单",
                            "医疗费用明细清单",
                            "费用明细清单",
                            "费用明细",
                            "清单计价表"),
                    new KeywordRule(
                            SpecializedMedicalOcrApi.MEDICAL_STATEMENT,
                            "医疗费用结算单",
                            "医保结算单",
                            "出院结算单",
                            "住院结算单",
                            "费用结算单"),
                    new KeywordRule(
                            SpecializedMedicalOcrApi.MEDICAL_INVOICE,
                            "医疗住院收费票据",
                            "医疗门诊收费票据",
                            "医疗收费票据",
                            "门诊收费票据",
                            "住院收费票据"),
                    new KeywordRule(
                            SpecializedMedicalOcrApi.HEALTH_REPORT,
                            "诊断报告书",
                            "诊断证明书",
                            "疾病诊断证明"));

    /**
     * @param fullText 高精度 OCR 合并全文（行间多为 {@code \n}）。匹配时会去掉空白再比对，避免标题被拆成「检验报告」+「单」两行导致漏命中。
     */
    public static Optional<SpecializedMedicalOcrApi> resolve(String fullText) {
        if (fullText == null || fullText.isBlank()) {
            return Optional.empty();
        }
        for (KeywordRule rule : RULES) {
            for (String phrase : rule.phrases()) {
                if (!phrase.isEmpty() && containsNormalized(fullText, phrase)) {
                    return Optional.of(rule.api());
                }
            }
        }
        return Optional.empty();
    }

    /** 原文包含，或去掉各类空白后包含（覆盖换行、空格拆开标题）。 */
    static boolean containsNormalized(String fullText, String phrase) {
        if (fullText.contains(phrase)) {
            return true;
        }
        String cf = collapseWs(fullText);
        String cp = collapseWs(phrase);
        return !cp.isEmpty() && cf.contains(cp);
    }

    private static String collapseWs(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.replace('\u00a0', ' ').replaceAll("\\s+", "");
    }
}
