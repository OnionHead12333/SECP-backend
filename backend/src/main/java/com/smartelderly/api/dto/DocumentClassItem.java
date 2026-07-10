package com.smartelderly.api.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 百度云 doc_classify 单个主体：类别 + 置信度 + 可选位置。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentClassItem {
    /** 类别中文名，如「医疗_门诊处方笺」 */
    private String type;
    /** 置信度 0~1；接口字段名为 probablity（拼写如此） */
    private Double probability;
    private Map<String, Object> location;
}
