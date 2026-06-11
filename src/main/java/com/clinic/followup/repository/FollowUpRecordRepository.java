package com.clinic.followup.repository;

import com.clinic.followup.entity.FollowUpRecord;
import com.clinic.followup.enums.CallResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FollowUpRecordRepository extends JpaRepository<FollowUpRecord, Long> {

    List<FollowUpRecord> findByPlanIdOrderByCreatedAtDesc(Long planId);

    @Query("SELECT r FROM FollowUpRecord r WHERE r.plan.id = :planId ORDER BY r.createdAt DESC LIMIT 1")
    Optional<FollowUpRecord> findLatestRecordByPlanId(@Param("planId") Long planId);

    @Query("SELECT COUNT(r) FROM FollowUpRecord r WHERE r.plan.id = :planId " +
           "AND r.callResult IN :results AND r.createdAt >= :since")
    long countFailedCallsSince(@Param("planId") Long planId,
                               @Param("results") List<CallResult> results,
                               @Param("since") LocalDateTime since);

    @Query("SELECT r FROM FollowUpRecord r WHERE r.nextReminderDate BETWEEN :startDate AND :endDate " +
           "ORDER BY r.nextReminderDate ASC")
    List<FollowUpRecord> findRecordsWithRemindersBetween(@Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    List<FollowUpRecord> findByPlanIdAndCallResultInOrderByCreatedAtDesc(Long planId, List<CallResult> results);

    @Query("SELECT r FROM FollowUpRecord r WHERE r.plan.id = :planId AND r.callResult = 'CONNECTED' " +
           "ORDER BY r.createdAt DESC LIMIT 1")
    Optional<FollowUpRecord> findLastSuccessfulRecord(@Param("planId") Long planId);
}
