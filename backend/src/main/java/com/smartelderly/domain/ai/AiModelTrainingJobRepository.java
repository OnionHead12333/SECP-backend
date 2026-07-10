package com.smartelderly.domain.ai;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiModelTrainingJobRepository extends JpaRepository<AiModelTrainingJob, Long> {
    Optional<AiModelTrainingJob> findTopByModelTypeOrderByCreatedAtDesc(String modelType);
}
