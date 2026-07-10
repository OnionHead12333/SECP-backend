package com.smartelderly.domain.ai;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "ai_medical_qa_knowledge")
public class AiMedicalQaKnowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "question_keywords", length = 1000)
    private String questionKeywords;

    @Column(name = "answer_summary", columnDefinition = "TEXT")
    private String answerSummary;

    @Column(name = "disease_tag", columnDefinition = "TEXT")
    private String diseaseTag;

    @Column(name = "symptom_tag", columnDefinition = "TEXT")
    private String symptomTag;

    @Column(name = "scene_tag", length = 100)
    private String sceneTag;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel = "low";

    @Column(name = "department_id")
    private Long departmentId;

    @Column(length = 100)
    private String source = "import_dataset";

    @Column(nullable = false, length = 20)
    private String status = "active";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (riskLevel == null || riskLevel.isBlank()) {
            riskLevel = "low";
        }
        if (source == null || source.isBlank()) {
            source = "import_dataset";
        }
        if (status == null || status.isBlank()) {
            status = "active";
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
