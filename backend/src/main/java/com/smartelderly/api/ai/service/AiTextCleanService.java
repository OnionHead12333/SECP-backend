package com.smartelderly.api.ai.service;

import org.springframework.stereotype.Service;

@Service
public class AiTextCleanService {

    public String cleanText(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replace('\u00A0', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("\\n+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    public String normalizeQuestionKey(String value) {
        String cleaned = cleanText(value);
        return cleaned == null ? null : cleaned.toLowerCase();
    }

    public boolean isTooShort(String value) {
        return value == null || cleanText(value).length() < 4;
    }
}
