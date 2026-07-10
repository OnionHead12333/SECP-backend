package com.smartelderly.api.ai.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiQueryUnderstandingService {

    private static final Logger log = LoggerFactory.getLogger(AiQueryUnderstandingService.class);

    public QueryUnderstandingResult understand(String inputText) {
        String text = inputText == null ? "" : inputText.trim();
        String lower = text.toLowerCase(Locale.ROOT);

        String symptomType = "unknown";
        double symptomConfidence = 0d;
        List<String> symptomKeywords = new ArrayList<>();
        String intentType = "unknown";
        double intentConfidence = 0d;
        List<String> dangerSignals = new ArrayList<>();
        List<String> followUpQuestions = new ArrayList<>();
        String unknownReason = null;

        if (containsAny(text, "胸痛", "胸闷", "呼吸困难", "喘不上气", "意识不清", "昏迷", "大出血", "黑便", "便血", "一侧肢体无力", "口角歪斜", "说不清话", "突然说不清话", "抽搐", "剧烈头痛", "摔倒后不能动")) {
            dangerSignals.add("high_risk_keyword");
        }

        if (containsAny(text, "头晕", "眩晕", "晕", "发晕", "晕乎", "头昏", "头重脚轻", "站不稳", "天旋地转", "眼前发黑", "发飘", "发虚") || "晕".equals(text)) {
            symptomType = "dizziness";
            symptomConfidence = 0.95d;
            symptomKeywords.add("头晕");
            symptomKeywords.add("晕");
            symptomKeywords.add("眩晕");
            followUpQuestions.add("是突然晕，还是已经持续一段时间？");
            followUpQuestions.add("是天旋地转，还是发飘发虚？");
            followUpQuestions.add("有没有恶心、呕吐、耳鸣、胸闷或手脚无力？");
        } else if (containsAny(text, "胸痛", "胸闷", "心口闷", "喘不上气", "呼吸困难")) {
            symptomType = "chest";
            symptomConfidence = 0.95d;
            symptomKeywords.add("胸痛");
            symptomKeywords.add("胸闷");
            symptomKeywords.add("呼吸困难");
            followUpQuestions.add("胸闷是持续还是阵发？");
            followUpQuestions.add("有没有出汗、心慌、放射到背部或左臂？");
        } else if (containsAny(text, "咳嗽", "咳痰", "痰多", "喘不上气", "呼吸困难", "发热", "发烧")) {
            symptomType = "fever_cough";
            symptomConfidence = 0.9d;
            symptomKeywords.add("咳嗽");
            symptomKeywords.add("发热");
            followUpQuestions.add("咳嗽持续多久了？");
            followUpQuestions.add("有没有发热、痰黄、呼吸困难？");
        } else if (containsAny(text, "腿疼", "膝盖疼", "膝盖痛", "走路疼", "腿痛", "关节疼", "脚痛", "腰疼", "腰痛", "摔倒")) {
            symptomType = "pain_fall";
            symptomConfidence = 0.9d;
            symptomKeywords.add("腿疼");
            symptomKeywords.add("膝盖疼");
            symptomKeywords.add("走路疼");
            followUpQuestions.add("是关节痛、肌肉痛还是走路时痛？");
            followUpQuestions.add("有没有肿胀、外伤、发热或活动受限？");
        } else if (containsAny(text, "肚子疼", "腹痛", "胃疼", "胃痛", "腹泻", "恶心", "呕吐")) {
            symptomType = "abdominal";
            symptomConfidence = 0.9d;
            symptomKeywords.add("腹痛");
            symptomKeywords.add("腹泻");
            followUpQuestions.add("疼痛在什么位置？");
            followUpQuestions.add("有没有发热、呕吐、腹泻或黑便？");
        } else if (containsAny(text, "失眠", "睡不着", "焦虑", "心烦", "睡不好", "烦躁")) {
            symptomType = "insomnia_anxiety";
            symptomConfidence = 0.88d;
            symptomKeywords.add("失眠");
            symptomKeywords.add("睡不着");
            followUpQuestions.add("睡不着是入睡困难还是容易醒？");
            followUpQuestions.add("最近有没有心烦、紧张、情绪波动？");
        } else if (containsAny(text, "尿频", "尿痛", "尿急", "小便多", "排尿疼")) {
            symptomType = "urinary";
            symptomConfidence = 0.88d;
            symptomKeywords.add("尿频");
            symptomKeywords.add("尿痛");
            followUpQuestions.add("有没有尿痛、尿急、发热或腰痛？");
            followUpQuestions.add("尿液颜色有没有变化？");
        } else if (containsAny(text, "便血", "黑便", "血便", "大便发黑")) {
            symptomType = "blood_stool";
            symptomConfidence = 0.96d;
            symptomKeywords.add("便血");
            symptomKeywords.add("黑便");
            followUpQuestions.add("大便是黑色还是带鲜红血？");
            followUpQuestions.add("有没有头晕、乏力、腹痛或呕吐？");
        } else if (containsAny(text, "不舒服", "怪怪的", "状态不对", "说不清", "不好受", "不太舒服", "老人说不清", "老人说不清楚")) {
            symptomType = "unknown";
            symptomConfidence = 0d;
            unknownReason = "文本过于模糊，缺少明确症状关键词";
            followUpQuestions.add("具体哪里不舒服？");
            followUpQuestions.add("是疼、晕、闷、喘、发热，还是其他感觉？");
            followUpQuestions.add("持续多久了？");
            followUpQuestions.add("有没有胸痛、呼吸困难、说话不清、手脚无力、摔倒、出血等情况？");
        }

        if (containsAny(text, "怎么办", "怎么处理", "怎么治", "如何处理", "怎么缓解")) {
            intentType = "advice";
            intentConfidence = 0.9d;
        } else if (containsAny(text, "挂什么科", "什么科", "看哪个科", "去哪个科")) {
            intentType = "department";
            intentConfidence = 0.92d;
        } else if (containsAny(text, "为什么", "原因", "怎么回事", "什么原因")) {
            intentType = "cause";
            intentConfidence = 0.88d;
        } else if (containsAny(text, "用药", "吃药", "药", "停药", "换药")) {
            intentType = "medication";
            intentConfidence = 0.84d;
        } else if (containsAny(text, "护理", "照护", "怎么照顾", "怎么护理")) {
            intentType = "nursing";
            intentConfidence = 0.84d;
        } else if (containsAny(text, "急救", "急症", "赶紧", "马上去医院", "立刻去医院")) {
            intentType = "emergency";
            intentConfidence = 0.9d;
        }

        boolean isTooShort = text.length() < 6;
        boolean isInformationEnough = !"unknown".equals(symptomType) && text.length() >= 4 && dangerSignals.isEmpty();
        boolean needFollowUp = "unknown".equals(symptomType) || (symptomConfidence > 0d && !isInformationEnough);

        log.info("AI understand originalText={}, normalizedText={}, symptomType={}, symptomKeywords={}, intentType={}, needFollowUp={}, dangerSignals={}",
                text, lower, symptomType, symptomKeywords, intentType, needFollowUp, dangerSignals);

        return new QueryUnderstandingResult(text, lower, symptomType, symptomConfidence, symptomKeywords,
                intentType, intentConfidence, dangerSignals, isInformationEnough, needFollowUp, followUpQuestions, unknownReason);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text != null && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public record QueryUnderstandingResult(
            String originalText,
            String normalizedText,
            String symptomType,
            double symptomConfidence,
            List<String> symptomKeywords,
            String intentType,
            double intentConfidence,
            List<String> dangerSignals,
            boolean isInformationEnough,
            boolean needFollowUp,
            List<String> followUpQuestions,
            String unknownReason) {
    }
}
