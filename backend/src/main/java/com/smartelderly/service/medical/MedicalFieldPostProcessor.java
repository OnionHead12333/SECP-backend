package com.smartelderly.service.medical;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * 医疗单据通用后处理器：
 * 1) 统一文本格式
 * 2) 修正常见 OCR 错字
 * 3) 做字段级正则清洗
 *
 * 这是通用版规则，不依赖特定医院样本，先覆盖最常见的医疗单据文本问题。
 */
@Service
public class MedicalFieldPostProcessor {

    private static final Pattern MULTI_SPACE = Pattern.compile("[\\t\\u000B\\f\\r ]+");
    private static final Pattern MULTI_NEWLINE = Pattern.compile("\\n{3,}");
    private static final Pattern DATE_CN = Pattern.compile("(\\d{4})\\s*年\\s*(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日");
    private static final Pattern DATE_NUM = Pattern.compile("(\\d{4})\\s*[-/.]\\s*(\\d{1,2})\\s*[-/.]\\s*(\\d{1,2})");
    private static final Pattern DATE_CN_COMPACT = Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})日");
    private static final Pattern DATE_NUM_COMPACT = Pattern.compile("(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})");
    private static final Pattern MG_SPLIT = Pattern.compile("(?i)(\\d)\\s+m\\s*g");
    private static final Pattern ML_SPLIT = Pattern.compile("(?i)(\\d)\\s+m\\s*l");
    private static final Pattern UNIT_SPLIT = Pattern.compile("(?i)(\\d)\\s*(mg|ml|g|kg|ug|μg|iu|miu|l)");

    private static final List<Map.Entry<String, String>> COMMON_REPLACEMENTS = List.of(
            Map.entry("１", "1"),
            Map.entry("０", "0"),
            Map.entry("Ｏ", "0"),
            Map.entry("o", "0"),
            Map.entry("O", "0"),
            Map.entry("ｌ", "l"),
            Map.entry("I", "1"),
            Map.entry("丨", "1"),
            Map.entry("／", "/"),
            Map.entry("－", "-"),
            Map.entry("—", "-"),
            Map.entry("：", ":"),
            Map.entry("（", "("),
            Map.entry("）", ")"),
            Map.entry("，", ","),
            Map.entry("。", "."));

    private static final Map<String, String> DICTIONARY_REPLACEMENTS = new LinkedHashMap<>();

    static {
        // 通用高频错字映射：优先放“确定性高”的规则
        DICTIONARY_REPLACEMENTS.put("檢查", "检查");
        DICTIONARY_REPLACEMENTS.put("检査", "检查");
        DICTIONARY_REPLACEMENTS.put("查验", "检查");
        DICTIONARY_REPLACEMENTS.put("阿莫西淋", "阿莫西林");
        DICTIONARY_REPLACEMENTS.put("阿莫西林克拉维酸甲", "阿莫西林克拉维酸钾");
        DICTIONARY_REPLACEMENTS.put("阿莫西林克拉维酸钾片", "阿莫西林克拉维酸钾片");
        DICTIONARY_REPLACEMENTS.put("布洛芬芬", "布洛芬");
        DICTIONARY_REPLACEMENTS.put("布洛芬缓释胶襄", "布洛芬缓释胶囊");
        DICTIONARY_REPLACEMENTS.put("对乙酷氨基酚", "对乙酰氨基酚");
        DICTIONARY_REPLACEMENTS.put("对乙酰氨基酚片", "对乙酰氨基酚片");
        DICTIONARY_REPLACEMENTS.put("头抱", "头孢");
        DICTIONARY_REPLACEMENTS.put("头孢克污", "头孢克肟");
        DICTIONARY_REPLACEMENTS.put("头孢呋幸", "头孢呋辛");
        DICTIONARY_REPLACEMENTS.put("氯化钠注射液", "氯化钠注射液");
        DICTIONARY_REPLACEMENTS.put("葡萄糖注射液", "葡萄糖注射液");
        DICTIONARY_REPLACEMENTS.put("检验报吿", "检验报告");
        DICTIONARY_REPLACEMENTS.put("检查报吿", "检查报告");
        DICTIONARY_REPLACEMENTS.put("报吿", "报告");
        DICTIONARY_REPLACEMENTS.put("化验单", "化验单");
        DICTIONARY_REPLACEMENTS.put("处方面", "处方单");
        DICTIONARY_REPLACEMENTS.put("处方笺", "处方笺");
        DICTIONARY_REPLACEMENTS.put("医嘱", "医嘱");
        DICTIONARY_REPLACEMENTS.put("门诊", "门诊");
        DICTIONARY_REPLACEMENTS.put("住院费川", "住院费用");
        DICTIONARY_REPLACEMENTS.put("住院费用", "住院费用");
        DICTIONARY_REPLACEMENTS.put("费甬", "费用");
        DICTIONARY_REPLACEMENTS.put("费用明细", "费用明细");
        DICTIONARY_REPLACEMENTS.put("发栗", "发票");
        DICTIONARY_REPLACEMENTS.put("发票", "发票");
        DICTIONARY_REPLACEMENTS.put("结算单", "结算单");
        DICTIONARY_REPLACEMENTS.put("医保", "医保");
        DICTIONARY_REPLACEMENTS.put("诊断", "诊断");
        DICTIONARY_REPLACEMENTS.put("病历", "病历");
        DICTIONARY_REPLACEMENTS.put("手术记录", "手术记录");
        DICTIONARY_REPLACEMENTS.put("出院小结", "出院小结");
        DICTIONARY_REPLACEMENTS.put("复诊", "复诊");
    }

    public ProcessedText process(String originalText) {
        String text = originalText == null ? "" : originalText;
        String normalized = normalizeText(text);
        normalized = applyCommonReplacements(normalized);
        normalized = applyDictionaryReplacements(normalized);
        normalized = cleanFieldFormats(normalized);
        return new ProcessedText(text, normalized);
    }

    private String normalizeText(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC);
        normalized = normalized.replace("\u00A0", " ");
        normalized = MULTI_SPACE.matcher(normalized).replaceAll(" ");
        normalized = MULTI_NEWLINE.matcher(normalized).replaceAll("\n\n");
        return normalized.trim();
    }

    private String applyCommonReplacements(String text) {
        String result = text;
        for (Map.Entry<String, String> entry : COMMON_REPLACEMENTS) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private String applyDictionaryReplacements(String text) {
        String result = text;
        for (Map.Entry<String, String> entry : DICTIONARY_REPLACEMENTS.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private String cleanFieldFormats(String text) {
        String result = text;
        result = DATE_CN.matcher(result).replaceAll("$1-$2-$3");
        result = DATE_NUM.matcher(result).replaceAll("$1-$2-$3");
        result = DATE_CN_COMPACT.matcher(result).replaceAll("$1-$2-$3");
        result = DATE_NUM_COMPACT.matcher(result).replaceAll("$1-$2-$3");
        result = MG_SPLIT.matcher(result).replaceAll("$1mg");
        result = ML_SPLIT.matcher(result).replaceAll("$1ml");
        result = UNIT_SPLIT.matcher(result).replaceAll("$1$2");
        result = result.replaceAll("(?i)1o", "10");
        result = result.replaceAll("(?i)o(?=mg|ml|g|kg|iu|miu)", "0");
        return result;
    }

    public static final class ProcessedText {
        private final String originalText;
        private final String cleanedText;

        public ProcessedText(String originalText, String cleanedText) {
            this.originalText = originalText;
            this.cleanedText = cleanedText;
        }

        public String getOriginalText() {
            return originalText;
        }

        public String getCleanedText() {
            return cleanedText;
        }
    }
}