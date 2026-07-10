package com.smartelderly.api.ai.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class AiTextPreprocessService {

    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern CHINESE_PUNCT = Pattern.compile("[，。！？；：、“”‘’（）【】《》…—·]");
    private static final Pattern EN_PUNCT = Pattern.compile("[\\p{Punct}]");

    public String normalize(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC);
        normalized = normalized.replace('，', ',').replace('。', '.').replace('！', '!').replace('？', '?')
                .replace('；', ';').replace('：', ':').replace('（', '(').replace('）', ')')
                .replace('【', '[').replace('】', ']').replace('“', '"').replace('”', '"')
                .replace('‘', '\'').replace('’', '\'');
        normalized = CHINESE_PUNCT.matcher(normalized).replaceAll(" ");
        normalized = EN_PUNCT.matcher(normalized).replaceAll(" ");
        normalized = MULTI_SPACE.matcher(normalized).replaceAll(" ").trim();
        return normalized;
    }

    public List<String> tokenize(String text) {
        String normalized = normalize(text).toLowerCase(Locale.ROOT);
        Set<String> tokens = new LinkedHashSet<>();
        if (normalized.isEmpty()) {
            return List.of();
        }
        for (String part : normalized.split(" ")) {
            if (part.isBlank()) {
                continue;
            }
            if (part.matches("[a-z0-9\\-]+")) {
                tokens.add(part);
                continue;
            }
            if (part.length() >= 2) {
                tokens.add(part);
                for (int i = 0; i < part.length() - 1; i++) {
                    String biGram = part.substring(i, i + 2);
                    if (!biGram.isBlank()) {
                        tokens.add(biGram);
                    }
                }
            } else {
                tokens.add(part);
            }
        }
        return new ArrayList<>(tokens);
    }
}
