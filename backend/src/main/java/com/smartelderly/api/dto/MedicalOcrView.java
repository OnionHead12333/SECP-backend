package com.smartelderly.api.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 医疗单据拍照 OCR：聚合可读全文与百度云原始 JSON（便于后续结构化抽取）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalOcrView {
    private String fullText;
    private Map<String, Object> raw;
    /** 百度云「文件检测分类」结果（可多主体）；未开通或失败时可能为空。 */
    private List<DocumentClassItem> documentClasses;
    /** 分类接口原始 JSON，便于调试 */
    private Map<String, Object> classifyRaw;
    /**
     * 根据 {@link #documentClasses} 主类别路由到的百度云结构化接口 id（如 {@code medical_prescription}）；
     * 未匹配或未分类时为 null。
     */
    private String routedSpecializedApi;
    /** 结构化接口返回体；未调用、失败或未开通时为 null */
    private Map<String, Object> specializedRaw;
    /** 结构化接口失败原因；未调用或成功时为 null */
    private String structuredError;
    /**
     * 当 OCR 全文与百度云 doc_classify 主类别明显不符时的提示（给用户看）。
     * 不命中全文关键词路由时，非空则不会再按分类调用结构化接口；若已按全文关键词路由，仍可能调用结构化接口。
     */
    private String classificationWarning;
    /** {@code keyword_text}：按 OCR 全文关键词；{@code doc_classify}：按文件检测分类；未调用结构化时为 null */
    private String structuredRouteSource;
}
