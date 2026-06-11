package com.clinic.followup.repository;

import com.clinic.followup.entity.FollowUpNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FollowUpNodeRepository extends JpaRepository<FollowUpNode, Long> {

    List<FollowUpNode> findByPlanIdOrderByNodeOrderAsc(Long planId);

    List<FollowUpNode> findByPlanIdAndCompletedFalseAndCancelledFalseOrderByFollowUpDateAsc(Long planId);

    @Query("SELECT n FROM FollowUpNode n WHERE n.plan.id = :planId AND n.completed = false " +
           "AND n.cancelled = false ORDER BY n.followUpDate ASC LIMIT 1")
    Optional<FollowUpNode> findNextPendingNode(@Param("planId") Long planId);

    @Query("SELECT n FROM FollowUpNode n WHERE n.followUpDate BETWEEN :startDate AND :endDate " +
           "AND n.completed = false AND n.cancelled = false ORDER BY n.followUpDate ASC")
    List<FollowUpNode> findNodesDueBetween(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(n) > 0 FROM FollowUpNode n WHERE n.plan.id = :planId " +
           "AND n.followUpDate = :date AND n.cancelled = false")
    boolean existsByPlanIdAndFollowUpDate(@Param("planId") Long planId, @Param("date") LocalDate date);
}
