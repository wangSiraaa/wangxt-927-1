package com.clinic.followup.repository;

import com.clinic.followup.entity.EscalationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EscalationHistoryRepository extends JpaRepository<EscalationHistory, Long> {

    List<EscalationHistory> findByPlanIdOrderByCreatedAtDesc(Long planId);

    @Query("SELECT e FROM EscalationHistory e WHERE e.plan.id = :planId AND e.resolved = false " +
           "ORDER BY e.createdAt DESC LIMIT 1")
    Optional<EscalationHistory> findActiveEscalation(@Param("planId") Long planId);

    @Query("SELECT COUNT(e) > 0 FROM EscalationHistory e WHERE e.plan.id = :planId AND e.resolved = false")
    boolean hasActiveEscalation(@Param("planId") Long planId);

    List<EscalationHistory> findByToDoctorAndResolvedFalseOrderByCreatedAtDesc(String toDoctor);

    @Query("SELECT e FROM EscalationHistory e WHERE e.plan.id = :planId ORDER BY e.createdAt DESC")
    List<EscalationHistory> findAllEscalationHistoryForPlan(@Param("planId") Long planId);
}
