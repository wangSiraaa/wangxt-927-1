package com.clinic.followup.repository;

import com.clinic.followup.entity.FollowUpPlan;
import com.clinic.followup.enums.PlanStatus;
import com.clinic.followup.enums.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FollowUpPlanRepository extends JpaRepository<FollowUpPlan, Long> {

    List<FollowUpPlan> findByPatientIdCard(String patientIdCard);

    List<FollowUpPlan> findByTransferStatus(TransferStatus transferStatus);

    List<FollowUpPlan> findByStatus(PlanStatus status);

    List<FollowUpPlan> findByStatusAndTransferStatus(PlanStatus status, TransferStatus transferStatus);

    @Query("SELECT p FROM FollowUpPlan p WHERE p.patientIdCard = :idCard AND p.status != 'CANCELLED' ORDER BY p.createdAt DESC")
    List<FollowUpPlan> findActivePlansByPatientIdCard(@Param("idCard") String patientIdCard);

    @Query("SELECT p FROM FollowUpPlan p WHERE p.status = 'ACTIVE' AND p.transferStatus = 'NOT_TRANSFERRED' " +
           "AND p.consecutiveMissed >= :threshold ORDER BY p.consecutiveMissed DESC")
    List<FollowUpPlan> findPlansNeedingEscalation(@Param("threshold") int threshold);

    Optional<FollowUpPlan> findByIdAndTransferStatus(Long id, TransferStatus transferStatus);

    @Query("SELECT COUNT(p) > 0 FROM FollowUpPlan p WHERE p.patientIdCard = :idCard " +
           "AND p.status IN ('ACTIVE', 'ESCALATED') AND p.transferStatus = 'NOT_TRANSFERRED'")
    boolean hasActivePlanForPatient(@Param("idCard") String patientIdCard);

    @Query("SELECT p FROM FollowUpPlan p WHERE p.assignedNurse = :nurseName " +
           "AND p.status IN ('ACTIVE', 'ESCALATED') ORDER BY p.updatedAt DESC")
    List<FollowUpPlan> findByAssignedNurse(@Param("nurseName") String nurseName);

    @Query("SELECT p FROM FollowUpPlan p WHERE p.attendingDoctor = :doctorName " +
           "AND p.status IN ('ACTIVE', 'ESCALATED') ORDER BY p.updatedAt DESC")
    List<FollowUpPlan> findByAttendingDoctor(@Param("doctorName") String doctorName);
}
