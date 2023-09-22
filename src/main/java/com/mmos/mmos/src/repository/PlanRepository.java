package com.mmos.mmos.src.repository;

import com.mmos.mmos.src.domain.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<List<Plan>> findPlansByPlanIsStudy(Boolean isStudy);
}
