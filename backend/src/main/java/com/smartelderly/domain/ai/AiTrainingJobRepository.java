package com.smartelderly.domain.ai;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiTrainingJobRepository extends JpaRepository<AiTrainingJob, Long> {

    Optional<AiTrainingJob> findTopByOrderByIdDesc();
}
