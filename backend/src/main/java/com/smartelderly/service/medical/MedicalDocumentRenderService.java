package com.smartelderly.service.medical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.smartelderly.api.dto.MedicalOcrView;
import com.smartelderly.api.dto.medical.ExtractedMedicalFieldsDto;
import com.smartelderly.api.dto.medical.MedicalDisplayBlockDto;
import com.smartelderly.domain.MedicalDocument;
import com.smartelderly.service.ocr.SpecializedMedicalOcrApi;

@Service
public class MedicalDocumentRenderService {

    private static final List<String> TITLE_KEYS = List.of(
            "hospital_name",
            "hospitalName",
            "institution",
            "organization",
            "title",
            "name",
            "医院",
            "医疗机构",
            "单位名称");

    private static final Map<String, List<String>> REPORT_FIELD_KEYS = Map.ofEntries(
            Map.entry("检验项目", List.of("test_item", "ItemName", "item_name", "testItem", "检验项目", "项目名称")),
            Map.entry("样本类型", List.of("sample_type", "SampleType", "sampleType", "样本类型", "标本类型")),
            Map.entry("数量", List.of("count", "Num", "num", "quantity", "数量")),
            Map.entry("执行科室", List.of("department", "exec_dept", "execDept", "执行科室", "科室")),
            Map.entry("标本要求", List.of("sample_requirement", "sampleRequirement", "标本要求", "样本要求")),
            Map.entry("姓名", List.of("Name", "name", "patient_name", "patientName", "姓名")),
            Map.entry("性别", List.of("Sex", "sex", "gender", "性别")),
            Map.entry("年龄", List.of("Age", "age", "年龄")),
            Map.entry("处方号", List.of("prescription_id", "prescriptionId", "RecipeNum", "recipeNum", "处方号")));

    private static final Map<String, List<String>> PRESCRIPTION_FIELD_KEYS = Map.ofEntries(
            Map.entry("处方号", List.of("prescription_id", "prescriptionId", "RecipeNum", "recipeNum", "处方号")),
            Map.entry("姓名", List.of("Name", "name", "patient_name", "patientName", "姓名")),
            Map.entry("性别", List.of("Sex", "sex", "gender", "性别")),
            Map.entry("年龄", List.of("Age", "age", "年龄")),
            Map.entry("科室", List.of("department", "dept", "department_name", "科室")),
            Map.entry("诊断", List.of("diagnosis", "diagnosisText", "diagnosis_name", "诊断")),
            Map.entry("用法", List.of("usage", "dosage", "用法")),
            Map.entry("药品名称", List.of("drug_name", "drugName", "medicine_name", "药品名称", "药名")),
            Map.entry("规格", List.of("spec", "specification", "规格")),
            Map.entry("数量", List.of("count", "Num", "num", "quantity", "数量")));

    public RenderedDocument render(MedicalDocument document, MedicalOcrView ocr, ExtractedMedicalFieldsDto extracted) {
        String normalizedText = ocr.getFullText() == null ? "" : ocr.getFullText();
        List<MedicalDisplayBlockDto> blocks = new ArrayList<>();
        Map<String, String> structuredFields = new LinkedHashMap<>();

        addTitles(blocks, findTitleCandidates(ocr.getSpecializedRaw(), normalizedText));

        if (ocr.getSpecializedRaw() != null && !ocr.getSpecializedRaw().isEmpty()) {
            Map<String, List<String>> fieldMap = fieldMapForApi(ocr.getRoutedSpecializedApi(), document.getDocCategory());
            boolean matchedAny = false;
            // 检测到 structured Item 表格时，优先按表格渲染，避免被固定 kv 规则提前消费
            // Item 可能在 specializedRaw.words_result.Item 或 specializedRaw.Item
            Map<String, Object> itemSource = ocr.getSpecializedRaw();
            if (ocr.getSpecializedRaw().containsKey("words_result")) {
                Object wr = ocr.getSpecializedRaw().get("words_result");
                if (wr instanceof Map<?, ?> m) {
                    itemSource = (Map<String, Object>) m;
                }
            }
            if (itemSource.containsKey("Item")) {
                matchedAny = addBaiduItemStructuredBlocks(blocks, structuredFields, itemSource);
            }
            if (!matchedAny) {
                matchedAny = addStructuredBlocks(blocks, structuredFields, ocr.getSpecializedRaw(), fieldMap);
            }
            if (!matchedAny) {
                matchedAny = addGenericKvBlocks(blocks, structuredFields, normalizedText);
            }
            if (!matchedAny) {
                addParagraphFallback(blocks, normalizedText);
            }
        } else {
            boolean matchedAny = false;
            // try table detection from raw OCR (positions) first
            if (ocr.getRaw() != null) {
                matchedAny = tryParseTableFromRaw(blocks, structuredFields, ocr.getRaw());
            }
            if (!matchedAny) {
                matchedAny = addGenericKvBlocks(blocks, structuredFields, normalizedText);
            }
            if (!matchedAny) {
                addParagraphFallback(blocks, normalizedText);
            }
        }

        if (blocks.isEmpty()) {
            blocks.add(MedicalDisplayBlockDto.builder().type("paragraph").text(normalizedText).build());
        }

        String structuredError = buildStructuredError(ocr);
        return new RenderedDocument(blocks, structuredFields, structuredError);
    }

    public RenderedDocument renderFromStored(
            MedicalDocument document,
            MedicalOcrView ocr,
            ExtractedMedicalFieldsDto extracted,
            List<MedicalDisplayBlockDto> storedBlocks,
            Map<String, String> storedStructuredFields,
            String storedStructuredError) {
        if (storedBlocks != null && !storedBlocks.isEmpty()) {
            return new RenderedDocument(storedBlocks, storedStructuredFields == null ? Map.of() : storedStructuredFields, storedStructuredError);
        }
        RenderedDocument computed = render(document, ocr, extracted);
        if (storedStructuredFields != null && !storedStructuredFields.isEmpty()) {
            computed = new RenderedDocument(computed.displayBlocks(), storedStructuredFields, computed.structuredError());
        }
        return computed;
    }

    private void addTitles(List<MedicalDisplayBlockDto> blocks, List<String> titles) {
        if (titles == null) {
            return;
        }
        for (String title : titles) {
            if (title == null || title.isBlank()) {
                continue;
            }
            blocks.add(MedicalDisplayBlockDto.builder().type("title").text(title.trim()).build());
        }
    }

    private List<String> findTitleCandidates(Map<String, Object> specializedRaw, String text) {
        Set<String> titles = new LinkedHashSet<>();
        for (String key : TITLE_KEYS) {
            String value = findStringByKey(specializedRaw, key);
            if (isTitleLike(value)) {
                titles.add(value.trim());
            }
        }
        if (titles.isEmpty()) {
            for (String line : splitLines(text)) {
                String trimmed = line.trim();
                if (isTitleLike(trimmed)) {
                    titles.add(trimmed);
                }
                if (titles.size() >= 3) {
                    break;
                }
            }
        }
        return new ArrayList<>(titles);
    }

    private boolean addStructuredBlocks(
            List<MedicalDisplayBlockDto> blocks,
            Map<String, String> structuredFields,
            Map<String, Object> raw,
            Map<String, List<String>> fieldMap) {
        boolean matchedAny = false;
        if (fieldMap == null || fieldMap.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, List<String>> entry : fieldMap.entrySet()) {
            String value = findFirstString(raw, entry.getValue());
            if (value == null || value.isBlank()) {
                continue;
            }
            String normalized = value.trim();
            blocks.add(MedicalDisplayBlockDto.builder().type("kv").label(entry.getKey()).value(normalized).build());
            structuredFields.put(entry.getKey(), normalized);
            matchedAny = true;
        }
        return matchedAny;
    }

    @SuppressWarnings("unchecked")
    private boolean addBaiduItemStructuredBlocks(
            List<MedicalDisplayBlockDto> blocks,
            Map<String, String> structuredFields,
            Map<String, Object> raw) {
        Object itemObj = raw.get("Item");
        if (!(itemObj instanceof List<?> itemRows)) {
            return false;
        }
        // itemRows: list of rows, each row is a list of cell maps
        List<List<Map<String, Object>>> rows = new ArrayList<>();
        for (Object r : itemRows) {
            if (!(r instanceof List<?> cells)) continue;
            List<Map<String, Object>> row = new ArrayList<>();
            for (Object c : cells) {
                if (c instanceof Map<?, ?> m) {
                    row.add((Map<String, Object>) m);
                }
            }
            if (!row.isEmpty()) rows.add(row);
        }
        if (rows.isEmpty()) return false;

        // find header row: detect by checking if word field is empty (headers have word_name but word is empty)
        int headerIdx = -1;
        for (int i = 0; i < Math.min(rows.size(), 3); i++) {
            boolean allCellsHaveEmptyWord = rows.get(i).stream().allMatch(cell -> {
                String w = asString(cell.get("word"));
                return w == null || w.isBlank();
            });
            // if all cells in this row have empty word field, it's likely a header row
            if (allCellsHaveEmptyWord && rows.get(i).stream().anyMatch(cell -> {
                String wn = asString(cell.get("word_name"));
                return wn != null && (wn.contains("项目") || wn.contains("结果") || wn.contains("单位"));
            })) {
                headerIdx = i;
                break;
            }
        }
        // if no header row found, assume data starts from row 0
        int dataStartIdx = (headerIdx < 0) ? 0 : headerIdx + 1;

        // build header labels from header row (if exists) or first data row
        List<String> header = new ArrayList<>();
        if (headerIdx >= 0) {
            for (Map<String, Object> cell : rows.get(headerIdx)) {
                String w = asString(cell.get("word_name"));
                header.add(w == null ? "" : w.trim());
            }
        } else {
            // infer headers from word_name of first data row
            for (Map<String, Object> cell : rows.get(0)) {
                String w = asString(cell.get("word_name"));
                header.add(w == null ? "" : w.trim());
            }
        }

        // find indexes for label and value columns
        int labelIdx = -1, valueIdx = -1;
        for (int i = 0; i < header.size(); i++) {
            String h = header.get(i);
            if (h == null) continue;
            String lower = h.toLowerCase(Locale.ROOT);
            if (labelIdx < 0 && (lower.contains("项目名称") || lower.contains("项目代号") || lower.contains("名称"))) {
                labelIdx = i;
            }
            if (valueIdx < 0 && (lower.contains("结果"))) {
                valueIdx = i;
            }
        }
        if (labelIdx < 0 || valueIdx < 0) {
            // heuristic: look for 项目名称 in word_name, result in word
            labelIdx = -1;
            valueIdx = -1;
            for (int i = 0; i < rows.get(dataStartIdx).size(); i++) {
                String wn = asString(rows.get(dataStartIdx).get(i).get("word_name"));
                if (labelIdx < 0 && wn != null && wn.contains("项目名称")) labelIdx = i;
                if (valueIdx < 0 && wn != null && wn.contains("结果")) valueIdx = i;
            }
            if (labelIdx < 0) labelIdx = 7; // fallback: assume 项目名称 is at index 7
            if (valueIdx < 0) valueIdx = 4; // fallback: assume 结果 is at index 4
        }

        boolean matched = false;
        for (int r = dataStartIdx; r < rows.size(); r++) {
            List<Map<String, Object>> row = rows.get(r);
            String label = labelIdx < row.size() ? asString(row.get(labelIdx).get("word")) : null;
            String value = valueIdx < row.size() ? asString(row.get(valueIdx).get("word")) : null;
            if ((label == null || label.isBlank()) && row.size() > 0) {
                // try project name column by scanning cells for non-empty word
                for (int c = 0; c < Math.min(row.size(), 8); c++) {
                    String w = asString(row.get(c).get("word"));
                    if (w != null && !w.isBlank() && w.length() <= 50) { label = w; break; }
                }
            }
            if (label != null) label = label.trim();
            if (value != null) value = value.trim();
            // For Baidu Item data, trust the structure - no need to validate label plausibility
            if (label != null && !label.isBlank() && value != null && !value.isBlank()) {
                addKv(blocks, structuredFields, normalizeLabel(label), value);
                matched = true;
            }
        }

        return matched;
    }

    private boolean addGenericKvBlocks(
            List<MedicalDisplayBlockDto> blocks,
            Map<String, String> structuredFields,
            String text) {
        boolean matchedAny = false;
        List<String> lines = splitLines(text);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            int colon = indexOfColon(line);
            if (colon > 0 && colon < line.length() - 1) {
                String label = normalizeLabel(line.substring(0, colon));
                String value = line.substring(colon + 1).trim();
                if (isPlausibleLabel(label) && !value.isBlank()) {
                    addKv(blocks, structuredFields, label, value);
                    matchedAny = true;
                    continue;
                }
            }
            if (i + 1 < lines.size()) {
                String next = lines.get(i + 1).trim();
                if (!next.isEmpty() && isPlausibleLabel(line) && !isPlausibleLabel(next)) {
                    addKv(blocks, structuredFields, normalizeLabel(line), next);
                    matchedAny = true;
                    i++;
                    continue;
                }
            }
            if (line.length() > 25) {
                blocks.add(MedicalDisplayBlockDto.builder().type("paragraph").text(line).build());
            }
        }
        return matchedAny;
    }

    private void addParagraphFallback(List<MedicalDisplayBlockDto> blocks, String text) {
        for (String line : splitLines(text)) {
            if (line != null && !line.isBlank()) {
                blocks.add(MedicalDisplayBlockDto.builder().type("paragraph").text(line.trim()).build());
            }
        }
    }

    private void addKv(
            List<MedicalDisplayBlockDto> blocks,
            Map<String, String> structuredFields,
            String label,
            String value) {
        String cleanedLabel = normalizeLabel(label);
        String cleanedValue = value.trim();
        blocks.add(MedicalDisplayBlockDto.builder().type("kv").label(cleanedLabel).value(cleanedValue).build());
        structuredFields.put(cleanedLabel, cleanedValue);
    }

    private Map<String, List<String>> fieldMapForApi(String routedSpecializedApi, String docCategory) {
        String route = routedSpecializedApi == null ? "" : routedSpecializedApi;
        if (route.contains("medical_report_detection") || "LAB_REPORT".equalsIgnoreCase(docCategory)) {
            return REPORT_FIELD_KEYS;
        }
        if (route.contains("medical_prescription") || "PRESCRIPTION".equalsIgnoreCase(docCategory)) {
            return PRESCRIPTION_FIELD_KEYS;
        }
        return Map.of();
    }

    private String buildStructuredError(MedicalOcrView ocr) {
        if (ocr.getRoutedSpecializedApi() == null || ocr.getRoutedSpecializedApi().isBlank()) {
            return null;
        }
        if (ocr.getSpecializedRaw() != null && !ocr.getSpecializedRaw().isEmpty()) {
            return null;
        }
        return switch (ocr.getRoutedSpecializedApi()) {
            case "medical_report_detection" -> "检验报告结构化接口未返回结果，可能未开通、欠费或版式不匹配";
            case "medical_prescription" -> "处方结构化接口未返回结果，可能未开通、欠费或版式不匹配";
            case "medical_invoice", "medical_detail", "medical_statement" ->
                    "费用类结构化接口未返回结果，可能未开通、欠费或版式不匹配";
            default -> "结构化接口未返回结果，可能未开通、欠费或版式不匹配";
        };
    }

    private List<String> splitLines(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split("\\R"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private int indexOfColon(String line) {
        int colon = line.indexOf('：');
        if (colon >= 0) {
            return colon;
        }
        return line.indexOf(':');
    }

    private String normalizeLabel(String label) {
        return label == null ? "" : label.replaceAll("\\s+", "").trim();
    }

    private boolean isPlausibleLabel(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = normalizeLabel(line);
        if (trimmed.isEmpty() || trimmed.length() > 20) {
            return false;
        }
        if (trimmed.matches(".*\\d.*") && trimmed.length() > 8) {
            return false;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        return lower.endsWith("项目")
                || lower.endsWith("类型")
                || lower.endsWith("科室")
                || lower.endsWith("说明")
                || lower.endsWith("要求")
                || lower.endsWith("结果")
                || lower.endsWith("年龄")
                || lower.endsWith("性别")
                || lower.endsWith("姓名")
                || lower.endsWith("号码")
                || lower.endsWith("编号")
                || lower.endsWith("日期")
                || lower.endsWith("处方")
                || lower.endsWith("样本")
                || lower.endsWith("诊断")
                || lower.endsWith("规格")
                || lower.endsWith("数量")
                || lower.endsWith("用法")
                || lower.endsWith("用量");
    }

    private boolean isTitleLike(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty() || trimmed.length() > 40) {
            return false;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        return trimmed.contains("医院")
                || trimmed.contains("门诊")
                || trimmed.contains("中心")
                || trimmed.contains("诊所")
                || trimmed.contains("检验")
                || trimmed.contains("处方")
                || trimmed.contains("报告")
                || lower.contains("hospital")
                || lower.contains("clinic");
    }

    private String findFirstString(Map<String, Object> raw, List<String> keys) {
        if (raw == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            String value = findStringByKey(raw, key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String findStringByKey(Map<String, Object> raw, String key) {
        if (raw == null || key == null || key.isBlank()) {
            return null;
        }
        return findStringByKeyRecursive(raw, key);
    }

    @SuppressWarnings("unchecked")
    private String findStringByKeyRecursive(Object node, String key) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null && key.equalsIgnoreCase(String.valueOf(entry.getKey()))) {
                    String value = asString(entry.getValue());
                    if (value != null && !value.isBlank()) {
                        return value;
                    }
                }
            }
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String nested = findStringByKeyRecursive(entry.getValue(), key);
                if (nested != null && !nested.isBlank()) {
                    return nested;
                }
            }
        } else if (node instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                String nested = findStringByKeyRecursive(item, key);
                if (nested != null && !nested.isBlank()) {
                    return nested;
                }
            }
        }
        return null;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            Object text = map.get("text");
            if (text == null) {
                text = map.get("value");
            }
            if (text == null) {
                text = map.get("words");
            }
            if (text != null) {
                return String.valueOf(text);
            }
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                String nested = asString(item);
                if (nested != null && !nested.isBlank()) {
                    return nested;
                }
            }
        }
        return String.valueOf(value);
    }

    // --- table parsing helpers ---

    private static final int ROW_CLUSTER_PX = 14;
    private static final int COL_CLUSTER_PX = 30;

    private record WordBox(String text, int left, int top, int width, int height) {}

    private boolean tryParseTableFromRaw(
            List<MedicalDisplayBlockDto> blocks, Map<String, String> structuredFields, Map<String, Object> raw) {
        List<WordBox> boxes = new ArrayList<>();
        collectWordBoxesRecursive(raw, boxes);
        if (boxes.isEmpty()) {
            return false;
        }

        // group into rows by center y
        Map<Integer, List<WordBox>> rows = new LinkedHashMap<>();
        for (WordBox b : boxes) {
            int centerY = b.top + b.height / 2;
            Integer key = findCloseKey(rows.keySet(), centerY, ROW_CLUSTER_PX);
            if (key == null) {
                rows.put(centerY, new ArrayList<>());
                key = centerY;
            }
            rows.get(key).add(b);
        }

        if (rows.size() < 2) {
            return false;
        }

        // compute column clusters by median left positions
        List<Integer> colPositions = new ArrayList<>();
        for (List<WordBox> row : rows.values()) {
            for (WordBox b : row) {
                colPositions.add(b.left);
            }
        }
        colPositions.sort(Integer::compareTo);
        List<Integer> colCenters = clusterPositions(colPositions, COL_CLUSTER_PX);
        if (colCenters.size() < 2) {
            return false;
        }

        // build rows ordered by y
        List<List<WordBox>> orderedRows = new ArrayList<>(rows.values());
        orderedRows.sort((a, b) -> Integer.compare(a.get(0).top, b.get(0).top));

        boolean matchedAny = false;
        for (List<WordBox> row : orderedRows) {
            row.sort((a, b) -> Integer.compare(a.left, b.left));
            Map<Integer, StringBuilder> cols = new LinkedHashMap<>();
            for (Integer c : colCenters) cols.put(c, new StringBuilder());
            for (WordBox w : row) {
                int closest = nearest(colCenters, w.left);
                StringBuilder sb = cols.get(closest);
                if (sb.length() > 0) sb.append(' ');
                sb.append(w.text.trim());
            }
            // try pairing columns: for each i use i as label and remaining as value
            for (int i = 0; i < colCenters.size() - 1; i++) {
                String label = cols.get(colCenters.get(i)).toString().trim();
                StringBuilder valueBuilder = new StringBuilder();
                for (int j = i + 1; j < colCenters.size(); j++) {
                    String s = cols.get(colCenters.get(j)).toString().trim();
                    if (!s.isEmpty()) {
                        if (valueBuilder.length() > 0) valueBuilder.append(' ');
                        valueBuilder.append(s);
                    }
                }
                String value = valueBuilder.toString().trim();
                if (!label.isEmpty() && !value.isEmpty() && isPlausibleLabel(label)) {
                    addKv(blocks, structuredFields, normalizeLabel(label), value);
                    matchedAny = true;
                    break;
                }
            }
        }

        return matchedAny;
    }

    @SuppressWarnings("unchecked")
    private void collectWordBoxesRecursive(Object node, List<WordBox> out) {
        if (node == null) return;
        if (node instanceof Iterable<?> it) {
            for (Object item : it) {
                collectWordBoxesRecursive(item, out);
            }
            return;
        }
        if (node instanceof Map<?, ?> map) {
            Map<String, Object> m = (Map<String, Object>) node;
            String text = null;
            if (m.containsKey("words") && m.get("words") instanceof String) text = (String) m.get("words");
            if (text == null && m.containsKey("text") && m.get("text") instanceof String) text = (String) m.get("text");
            if (text == null && m.containsKey("word") && m.get("word") instanceof String) text = (String) m.get("word");

            Object loc = null;
            if (m.containsKey("location")) loc = m.get("location");
            if (loc == null && m.containsKey("loc")) loc = m.get("loc");
            if (loc == null && m.containsKey("position")) loc = m.get("position");

            if (text != null && loc instanceof Map<?, ?>) {
                Map<String, Object> l = (Map<String, Object>) loc;
                Integer left = toInt(l.get("left"));
                Integer top = toInt(l.get("top"));
                Integer width = toInt(l.get("width"));
                Integer height = toInt(l.get("height"));
                if (left == null) left = toInt(l.get("x"));
                if (top == null) top = toInt(l.get("y"));
                if (width == null) width = toInt(l.get("w"));
                if (height == null) height = toInt(l.get("h"));
                if (left != null && top != null && width != null && height != null) {
                    out.add(new WordBox(text, left, top, width, height));
                }
            }

            for (Object v : m.values()) {
                collectWordBoxesRecursive(v, out);
            }
        }
    }

    private Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer findCloseKey(Set<Integer> keys, int value, int tol) {
        for (Integer k : keys) {
            if (Math.abs(k - value) <= tol) return k;
        }
        return null;
    }

    private List<Integer> clusterPositions(List<Integer> positions, int tol) {
        List<Integer> centers = new ArrayList<>();
        if (positions.isEmpty()) return centers;
        int sum = positions.get(0);
        int cnt = 1;
        for (int i = 1; i < positions.size(); i++) {
            int v = positions.get(i);
            if (Math.abs(v - positions.get(i - 1)) <= tol) {
                sum += v;
                cnt++;
            } else {
                centers.add(sum / cnt);
                sum = v;
                cnt = 1;
            }
        }
        centers.add(sum / cnt);
        return centers;
    }

    private int nearest(List<Integer> centers, int x) {
        int best = centers.get(0);
        int bestDist = Math.abs(best - x);
        for (int c : centers) {
            int d = Math.abs(c - x);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        return best;
    }

    public record RenderedDocument(
            List<MedicalDisplayBlockDto> displayBlocks,
            Map<String, String> structuredFields,
            String structuredError) {}

    /**
     * 将 Baidu 风格的 specializedRaw `Item` 二维数组抽取为表格行文本矩阵（不做列对齐，只按单元文本返回）。
     */
    @SuppressWarnings("unchecked")
    public List<List<String>> extractTableRowsFromSpecializedRaw(Map<String, Object> raw) {
        List<List<String>> rowsOut = new ArrayList<>();
        if (raw == null) return rowsOut;
        
        // Item 可能在 raw.Item 或 raw.words_result.Item
        Map<String, Object> itemSource = raw;
        if (raw.containsKey("words_result")) {
            Object wr = raw.get("words_result");
            if (wr instanceof Map<?, ?> m) {
                itemSource = (Map<String, Object>) m;
            }
        }
        
        Object itemObj = itemSource.get("Item");
        if (!(itemObj instanceof List<?> itemRows)) return rowsOut;
        for (Object r : itemRows) {
            if (!(r instanceof List<?> cells)) continue;
            List<String> rowOut = new ArrayList<>();
            for (Object c : cells) {
                if (c instanceof Map<?, ?> m) {
                    // Priority: word (actual value) > word_name (column label)
                    String w = asString(((Map<String, Object>) m).get("word"));
                    if (w == null || w.isBlank()) w = asString(((Map<String, Object>) m).get("word_name"));
                    rowOut.add(w == null ? "" : w.trim());
                } else {
                    rowOut.add(asString(c));
                }
            }
            if (!rowOut.isEmpty()) rowsOut.add(rowOut);
        }
        return rowsOut;
    }
}
