package com.smartelderly.service.medical;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.smartelderly.api.dto.MedicalOcrView;
import com.smartelderly.api.dto.medical.ExtractedMedicalFieldsDto;
import com.smartelderly.api.dto.medical.SuggestedCalendarEventDto;

@Service
public class MedicalFieldExtractionService {

    private static final Pattern CN_DATE =
            Pattern.compile("(\\d{4})\\s*年\\s*(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日");
    private static final Pattern NUM_DATE = Pattern.compile("(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})");

    public ExtractedMedicalFieldsDto extract(MedicalOcrView ocr) {
        String text = ocr.getFullText() == null ? "" : ocr.getFullText();
        String compact = text.replaceAll("\\s+", "");

        ExtractedMedicalFieldsDto dto = new ExtractedMedicalFieldsDto();
        dto.setDocCategory(guessDocCategory(ocr.getRoutedSpecializedApi(), compact));

        Set<String> rawDates = new LinkedHashSet<>();
        Set<String> isoDates = new LinkedHashSet<>();

        Matcher m1 = CN_DATE.matcher(text);
        while (m1.find()) {
            rawDates.add(m1.group());
            try {
                LocalDate d =
                        LocalDate.of(
                                Integer.parseInt(m1.group(1)),
                                Integer.parseInt(m1.group(2)),
                                Integer.parseInt(m1.group(3)));
                isoDates.add(d.toString());
            } catch (Exception ignored) {
                // skip unparsable
            }
        }
        Matcher m2 = NUM_DATE.matcher(text);
        while (m2.find()) {
            rawDates.add(m2.group());
            try {
                LocalDate d =
                        LocalDate.of(
                                Integer.parseInt(m2.group(1)),
                                Integer.parseInt(m2.group(2)),
                                Integer.parseInt(m2.group(3)));
                isoDates.add(d.toString());
            } catch (Exception ignored) {
                // skip
            }
        }

        dto.setDetectedDateTexts(new ArrayList<>(rawDates));
        dto.setNormalizedDates(new ArrayList<>(isoDates));

        List<String> kw = new ArrayList<>();
        String[] keys = {
            "检验报告单",
            "检查报告",
            "处方",
            "复诊",
            "入院",
            "出院小结",
            "诊断",
            "手术记录",
            "费用明细",
            "结算单",
            "票据"
        };
        for (String k : keys) {
            if (compact.contains(k.replaceAll("\\s+", ""))) {
                kw.add(k);
            }
        }
        dto.setMatchedKeywords(kw);
        return dto;
    }

    public List<SuggestedCalendarEventDto> suggestEvents(ExtractedMedicalFieldsDto extracted, MedicalOcrView ocr) {
        List<SuggestedCalendarEventDto> list = new ArrayList<>();
        List<String> iso = extracted.getNormalizedDates();
        if (iso == null || iso.isEmpty()) {
            return list;
        }

        LocalDate today = LocalDate.now();
        LocalDate picked = null;
        for (String s : iso) {
            LocalDate d = LocalDate.parse(s);
            if (!d.isBefore(today)) {
                picked = d;
                break;
            }
        }
        if (picked == null) {
            picked = LocalDate.parse(iso.get(0));
        }

        LocalDateTime start = LocalDateTime.of(picked, LocalTime.of(9, 0));
        String compact =
                (ocr.getFullText() == null ? "" : ocr.getFullText()).replaceAll("\\s+", "");

        String type;
        String title;
        if (compact.contains("复诊")) {
            type = "FOLLOWUP";
            title = "复诊提醒（来自单据解析）";
        } else if (compact.contains("处方")
                || compact.contains("用药")
                || compact.contains("服用")) {
            type = "MEDICATION";
            title = "用药相关备忘（请核对处方）";
        } else if (compact.contains("检验")
                || compact.contains("检查")
                || compact.contains("化验")
                || "LAB_REPORT".equalsIgnoreCase(extracted.getDocCategory())) {
            type = "EXAM";
            title = "检查 / 检验备忘（来自单据解析）";
        } else {
            type = "EXAM";
            title = "医疗日程备忘（来自单据日期）";
        }

        list.add(
                SuggestedCalendarEventDto.builder()
                        .eventType(type)
                        .title(title)
                        .startAt(start)
                        .notes("由智能识别生成，请在日历中确认或修改时间")
                        .build());
        return list;
    }

    private static String guessDocCategory(String routedApi, String compact) {
        if (routedApi == null || routedApi.isBlank()) {
            return heuristicCategory(compact);
        }
        return switch (routedApi) {
            case "medical_report_detection" -> "LAB_REPORT";
            case "medical_prescription" -> "PRESCRIPTION";
            case "medical_invoice",
                    "medical_detail",
                    "medical_statement" -> "BILLING";
            case "medical_outpatient",
                    "medical_record",
                    "medical_summary",
                    "medical_summary_in_hospital",
                    "health_report",
                    "medical_surgery" -> "RECORD";
            default -> heuristicCategory(compact);
        };
    }

    private static String heuristicCategory(String compact) {
        String c = compact.toLowerCase(Locale.ROOT);
        if (c.contains("检验报告") || c.contains("检查报告")) {
            return "LAB_REPORT";
        }
        if (c.contains("处方")) {
            return "PRESCRIPTION";
        }
        if (c.contains("发票") || c.contains("收费票据") || c.contains("费用明细")) {
            return "BILLING";
        }
        return "OTHER";
    }
}
