package com.smartelderly.service.ocr;

import java.util.Locale;
import java.util.Optional;

/**
 * 百度云 doc_classify 会误判（如笔记本扉页、笔记纸）。结合 OCR 全文做极简启发式，给出用户提示并在必要时跳过结构化接口。
 */
public final class MedicalClassificationConsistency {

    private MedicalClassificationConsistency() {}

    /**
     * @return 若非 empty，应对用户展示；同时业务流程应跳过专用结构化识别以免错误计费/错误字段。
     */
    public static Optional<String> conflictHint(String docClassifyType, String ocrFullText) {
        if (docClassifyType == null || docClassifyType.isBlank()) {
            return Optional.empty();
        }
        final String t = docClassifyType.trim();
        final String f = ocrFullText == null ? "" : ocrFullText.trim();
        if (f.length() < 12) {
            return Optional.empty();
        }

        final boolean cloudMedicalTicket =
                t.startsWith("医疗_") || t.contains("处方") || t.contains("病历");

        if (!cloudMedicalTicket) {
            return Optional.empty();
        }

        final boolean medicalCueInText = containsAny(f, "医院", "卫生院", "诊所", "门诊部", "科室", "门诊", "住院", "病历号", "病案号", "医嘱", "诊断证明书", "医保", "统筹", "收费章")
                || (f.contains("诊断") && !f.contains("图形"));

        final boolean academicCue = looksLikeAcademicOrNotebook(f);

        if (academicCue && !medicalCueInText) {
            return Optional.of(
                    "识别全文更像笔记、讲义或非正式单据，与当前自动分类可能不符；分类仅供参考。本次已跳过专用结构化接口，请以下方全文为准。");
        }

        return Optional.empty();
    }

    private static boolean looksLikeAcademicOrNotebook(String f) {
        final String u = f.toUpperCase(Locale.ROOT);
        if (containsAny(f, "北京交通大学", "上海交通大学", "清华大学", "北京大学", "浙江大学")) {
            if (!containsAny(f, "医学院附属", "附属医院")) {
                return true;
            }
        }
        if (u.contains("BEIJING JIAOTONG") || u.contains("TSINGHUA UNIVERSITY") || u.contains("PEKING UNIVERSITY")) {
            return true;
        }
        return containsAny(f, "课件", "习题", "作业", "复习提纲", "期末考试", "高等数学", "线性代数", "Chapter ")
                || (f.contains("大学") && containsAny(f, "讲义", "笔记", "习题课"));
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String n : needles) {
            if (haystack.contains(n)) {
                return true;
            }
        }
        return false;
    }
}
