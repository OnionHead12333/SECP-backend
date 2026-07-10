package com.smartelderly.api.ai.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.smartelderly.api.ai.dto.MatchedQaDTO;
import com.smartelderly.api.ai.dto.RecommendedDepartmentDTO;

@Service
public class AiAnswerGenerateService {

    public String generateNormalAnswer(String originalText, String symptomType, String riskLevel,
            List<MatchedQaDTO> matchedQaList, List<RecommendedDepartmentDTO> departments,
            boolean needFollowUp, List<String> followUpQuestions, List<String> dangerSignals) {
        StringBuilder sb = new StringBuilder();
        sb.append("【简单说明】\n");
        if ("dizziness".equalsIgnoreCase(symptomType)) {
            sb.append("您说的“头晕”原因比较多，可能和休息不足、血压波动、低血糖、耳部问题、贫血等有关。因为头晕有时也可能和脑血管问题有关，需要先确认一些情况。\n");
        } else if ("pain_fall".equalsIgnoreCase(symptomType)) {
            sb.append("您说的膝盖疼、走路疼，可能和关节、肌肉劳损或近期扭伤有关，仅凭当前描述还不能确定具体原因。\n");
        } else if ("insomnia_anxiety".equalsIgnoreCase(symptomType)) {
            sb.append("您说的睡不着，可能和作息、情绪、压力、身体不适或睡眠习惯有关。\n");
        } else {
            sb.append("结合当前描述，暂时还不能判断具体原因，需要先补充一些信息。\n");
        }

        sb.append("\n【建议先做】\n");
        sb.append(buildAdvice(symptomType, riskLevel));

        sb.append("\n【需要尽快就医的情况】\n");
        sb.append(buildVisitAdvice(dangerSignals, riskLevel, symptomType));

        sb.append("\n【建议科室】\n");
        sb.append(joinDepartments(departments));

        if (needFollowUp && followUpQuestions != null && !followUpQuestions.isEmpty()) {
            sb.append("\n\n【还需要了解】\n");
            for (int i = 0; i < Math.min(4, followUpQuestions.size()); i++) {
                sb.append(i + 1).append(". ").append(followUpQuestions.get(i)).append("\n");
            }
        }

        sb.append("\n【安全提示】\n本回答仅供健康咨询参考，不能替代医生诊断。");
        return sb.toString();
    }

    public String generateHighRiskAnswer(List<RecommendedDepartmentDTO> departments) {
        return "【风险提醒】\n您描述的情况可能存在较高风险，建议尽快就医。\n\n" +
                "【建议立即做】\n1. 先停止活动，保持安全姿势。\n2. 尽快联系家属或紧急联系人。\n3. 如果症状明显或持续加重，请及时前往急诊。\n\n" +
                "【建议科室】\n" + joinDepartments(departments) + "\n\n" +
                "【安全提示】\n本回答仅供健康咨询参考，不能替代医生诊断。";
    }

    public String generateSymptomFollowUpAnswer(String originalText, String symptomType, String riskLevel,
            List<MatchedQaDTO> matchedQaList, List<RecommendedDepartmentDTO> departments,
            boolean needFollowUp, List<String> followUpQuestions, List<String> dangerSignals) {
        StringBuilder sb = new StringBuilder();
        if ("dizziness".equalsIgnoreCase(symptomType)) {
            sb.append("【简单说明】\n您说的“晕”可能和休息不足、血压波动、低血糖、耳部问题、贫血等有关。因为头晕也可能和脑血管问题有关，需要先确认一些情况。\n\n");
            sb.append("【建议先做】\n1. 先坐下或躺下休息，避免突然站起。\n2. 可以测一下血压、血糖。\n3. 先不要自行乱吃药，也不要突然停药。\n\n");
            sb.append("【需要尽快就医的情况】\n如果是突然头晕，或者伴随说话不清、手脚无力、走路不稳、胸痛、呼吸困难，请尽快去急诊。\n\n");
            sb.append("【还需要了解】\n1. 是突然晕，还是已经持续一段时间？\n2. 是天旋地转，还是发飘发虚？\n3. 有没有恶心、呕吐、耳鸣、胸闷或手脚无力？\n\n");
        } else if ("pain_fall".equalsIgnoreCase(symptomType)) {
            sb.append("【简单说明】\n您说的膝盖疼、走路疼，可能和关节、肌肉劳损或近期扭伤有关，仅凭当前描述还不能确定具体原因。\n\n");
            sb.append("【建议先做】\n1. 先减少走动，避免继续负重。\n2. 观察有没有红肿、明显疼痛或活动受限。\n3. 如果疼痛持续不缓解，建议就医评估。\n\n");
            sb.append("【需要尽快就医的情况】\n如果是摔倒后明显疼痛、不能走路、关节变形或肿胀加重，请尽快就医。\n\n");
            sb.append("【建议科室】\n建议优先咨询骨科。\n\n");
            sb.append("【还需要了解】\n1. 疼痛持续多久了？\n2. 有没有摔倒、扭伤或突然加重？\n3. 膝盖有没有红肿、发热或不能弯曲？\n\n");
        } else if ("insomnia_anxiety".equalsIgnoreCase(symptomType)) {
            sb.append("【简单说明】\n您说的睡不着，可能和作息、情绪、压力、身体不适或睡眠习惯有关。\n\n");
            sb.append("【建议先做】\n1. 先保持规律作息，睡前减少刺激。\n2. 注意情绪和白天精神状态。\n3. 如果长期睡不好或影响白天生活，建议就医。\n\n");
            sb.append("【需要尽快就医的情况】\n如果长期睡不好并明显影响白天生活，或者伴随明显焦虑、心慌、情绪异常，请尽快就医。\n\n");
            sb.append("【还需要了解】\n1. 睡不着是入睡困难还是容易醒？\n2. 持续多久了？\n3. 最近有没有心烦、紧张、情绪波动？\n\n");
        } else {
            sb.append("【简单说明】\n结合当前描述，暂时还不能判断具体原因，需要先补充一些信息。\n\n");
            sb.append("【建议先做】\n1. 先注意休息，观察症状变化。\n2. 记录发作时间、诱因和伴随症状。\n3. 如果症状持续、加重，建议尽快就医。\n\n");
            sb.append("【需要尽快就医的情况】\n如果症状持续不缓解、明显加重，或者影响走路、呼吸、进食和睡眠，请尽快就医。\n\n");
            sb.append("【还需要了解】\n");
            if (followUpQuestions != null && !followUpQuestions.isEmpty()) {
                for (int i = 0; i < Math.min(4, followUpQuestions.size()); i++) {
                    sb.append(i + 1).append(". ").append(followUpQuestions.get(i)).append("\n");
                }
            } else {
                sb.append("1. 具体哪里不舒服？\n2. 是疼、晕、闷、喘、发热，还是其他感觉？\n3. 持续多久了？\n4. 有没有胸痛、呼吸困难、说话不清、手脚无力、摔倒、出血等情况？\n");
            }
        }
        sb.append("\n【安全提示】\n本回答仅供健康咨询参考，不能替代医生诊断。");
        return sb.toString();
    }

    public String generateUnknownFollowUpAnswer(List<String> followUpQuestions) {
        StringBuilder sb = new StringBuilder();
        sb.append("【需要补充信息】\n您描述的信息还不够具体，我还不能准确判断应该往哪个方向建议。\n\n");
        sb.append("【请您补充】\n");
        if (followUpQuestions != null && !followUpQuestions.isEmpty()) {
            for (int i = 0; i < Math.min(4, followUpQuestions.size()); i++) {
                sb.append(i + 1).append(". ").append(followUpQuestions.get(i)).append("\n");
            }
        } else {
            sb.append("1. 具体哪里不舒服？\n2. 是疼、晕、闷、喘、发热，还是其他感觉？\n3. 持续多久了？\n4. 有没有胸痛、呼吸困难、说话不清、手脚无力、摔倒、出血等情况？\n");
        }
        sb.append("\n【先做什么】\n如果现在症状明显加重，请先联系家属或及时就医。\n\n");
        sb.append("【安全提示】\n本回答仅供健康咨询参考，不能替代医生诊断。");
        return sb.toString();
    }

    public String generateNeedMoreInfoAnswer(List<String> followUpQuestions) {
        StringBuilder sb = new StringBuilder();
        sb.append("【需要补充信息】\n您描述的信息还不够具体，我还不能准确判断应该往哪个方向建议。\n\n");
        sb.append("【请您补充】\n");
        if (followUpQuestions != null && !followUpQuestions.isEmpty()) {
            for (int i = 0; i < Math.min(4, followUpQuestions.size()); i++) {
                sb.append(i + 1).append(". ").append(followUpQuestions.get(i)).append("\n");
            }
        } else {
            sb.append("1. 具体哪里不舒服？\n2. 是疼、晕、闷、喘、发热，还是其他感觉？\n3. 持续多久了？\n4. 有没有胸痛、呼吸困难、说话不清、手脚无力、摔倒、出血等情况？\n");
        }
        sb.append("\n【先做什么】\n如果现在症状明显加重，请先联系家属或及时就医。\n\n");
        sb.append("【安全提示】\n本回答仅供健康咨询参考，不能替代医生诊断。");
        return sb.toString();
    }

    private String buildAdvice(String symptomType, String riskLevel) {
        if ("high".equalsIgnoreCase(riskLevel) || "emergency".equalsIgnoreCase(riskLevel)) {
            return "1. 先停止活动，避免独自外出。\n2. 观察是否有加重趋势。\n3. 如有明显不适，尽快就医。";
        }
        if ("dizziness".equalsIgnoreCase(symptomType)) {
            return "1. 先坐下或躺下休息，避免突然起身。\n2. 可以测一下血压、血糖。\n3. 先不要自行乱吃药，也不要突然停药。";
        }
        if ("pain_fall".equalsIgnoreCase(symptomType)) {
            return "1. 先减少走动，避免继续负重。\n2. 观察有没有红肿、明显疼痛或活动受限。\n3. 如果疼痛持续不缓解，建议就医评估。";
        }
        if ("insomnia_anxiety".equalsIgnoreCase(symptomType)) {
            return "1. 先保持规律作息，睡前减少刺激。\n2. 注意情绪和白天精神状态。\n3. 如果长期睡不好或影响白天生活，建议就医。";
        }
        if ("fever_cough".equalsIgnoreCase(symptomType)) {
            return "1. 注意休息和补水。\n2. 观察体温、咳嗽和呼吸情况。\n3. 如果发热持续、咳嗽加重或出现呼吸困难，请尽快就医。";
        }
        if ("abdominal".equalsIgnoreCase(symptomType)) {
            return "1. 先观察腹痛位置和是否伴随腹泻、呕吐。\n2. 饮食先清淡，避免刺激性食物。\n3. 如果腹痛明显加重、反复呕吐或黑便，请尽快就医。";
        }
        return "1. 先注意休息，观察症状变化。\n2. 记录发作时间、诱因和伴随症状。\n3. 如果症状持续、加重，建议尽快就医。";
    }

    private String buildVisitAdvice(List<String> dangerSignals, String riskLevel, String symptomType) {
        if (dangerSignals != null && !dangerSignals.isEmpty()) {
            return "如果出现胸痛、呼吸困难、说话不清、手脚无力、摔倒后不能动、持续高热、出血或意识异常，请尽快就医。";
        }
        if ("dizziness".equalsIgnoreCase(symptomType)) {
            return "如果头晕是突然出现的，或者伴随说话不清、手脚无力、走路不稳、胸痛、呼吸困难，请尽快去急诊。";
        }
        if ("pain_fall".equalsIgnoreCase(symptomType)) {
            return "如果是摔倒后明显疼痛、不能走路、关节变形或肿胀加重，请尽快就医。";
        }
        if ("insomnia_anxiety".equalsIgnoreCase(symptomType)) {
            return "如果长期睡不好并明显影响白天生活，或者伴随明显焦虑、心慌、情绪异常，请尽快就医。";
        }
        if ("high".equalsIgnoreCase(riskLevel) || "emergency".equalsIgnoreCase(riskLevel)) {
            return "如症状持续加重或出现胸闷、喘不上气、意识异常等情况，请立即就医。";
        }
        return "如果症状持续不缓解、明显加重，或者影响走路、呼吸、进食和睡眠，请尽快就医。";
    }

    private String joinDepartments(List<RecommendedDepartmentDTO> departments) {
        if (departments == null || departments.isEmpty()) {
            return "全科医学科 / 内科 / 导诊台";
        }
        return departments.stream()
                .map(RecommendedDepartmentDTO::getDepartmentName)
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .limit(3)
                .collect(Collectors.joining(" / "));
    }
}
