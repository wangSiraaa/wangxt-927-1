package com.clinic.followup.service;

import com.clinic.followup.dto.EscalateRequest;
import com.clinic.followup.dto.ResolveEscalationRequest;
import com.clinic.followup.entity.EscalationHistory;
import com.clinic.followup.entity.FollowUpPlan;
import com.clinic.followup.enums.PlanStatus;
import com.clinic.followup.enums.TransferStatus;
import com.clinic.followup.exception.BusinessException;
import com.clinic.followup.exception.ResourceNotFoundException;
import com.clinic.followup.repository.EscalationHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EscalationService {

    private static final Logger log = LoggerFactory.getLogger(EscalationService.class);

    private final EscalationHistoryRepository escalationRepository;
    private final FollowUpPlanService planService;

    public EscalationService(EscalationHistoryRepository escalationRepository,
                             FollowUpPlanService planService) {
        this.escalationRepository = escalationRepository;
        this.planService = planService;
    }

    @Transactional
    public EscalationHistory createEscalation(EscalateRequest request) {
        FollowUpPlan plan = planService.getPlanById(request.getPlanId());

        if (plan.getTransferStatus() == TransferStatus.TRANSFERRED) {
            throw new BusinessException("已转院患者不能创建升级");
        }

        if (escalationRepository.hasActiveEscalation(request.getPlanId())) {
            throw new BusinessException("该计划已有未处理的升级记录");
        }

        EscalationHistory escalation = new EscalationHistory();
        escalation.setPlan(plan);
        escalation.setEscalationType(request.getEscalationType());
        escalation.setFromRole(request.getFromRole());
        escalation.setToDoctor(request.getToDoctor());
        escalation.setReason(request.getReason());
        escalation.setResolved(false);

        plan.setStatus(PlanStatus.ESCALATED);

        log.info("Plan {} escalated to doctor {} by {} - reason: {}",
                plan.getId(), request.getToDoctor(), request.getFromRole(), request.getReason());

        return escalationRepository.save(escalation);
    }

    @Transactional
    public EscalationHistory resolveEscalation(ResolveEscalationRequest request) {
        EscalationHistory escalation = getEscalationById(request.getEscalationId());

        if (escalation.isResolved()) {
            throw new BusinessException("该升级记录已处理");
        }

        escalation.setDoctorNote(request.getDoctorNote());
        escalation.setResolution(request.getResolution());
        escalation.setResolved(true);
        escalation.setResolvedAt(LocalDateTime.now());

        FollowUpPlan plan = escalation.getPlan();
        if (request.isResumeFollowUp()) {
            planService.resumeFromEscalation(plan);
            log.info("Escalation {} resolved, plan {} resumed to active",
                    escalation.getId(), plan.getId());
        } else {
            plan.setStatus(PlanStatus.CLOSED);
            log.info("Escalation {} resolved, plan {} closed",
                    escalation.getId(), plan.getId());
        }

        log.info("Escalation {} resolved by doctor. Resolution: {}",
                escalation.getId(), request.getResolution());

        return escalationRepository.save(escalation);
    }

    @Transactional
    public void closePlanByNurse(Long planId, String closeReason) {
        FollowUpPlan plan = planService.getPlanById(planId);

        if (plan.getStatus() == PlanStatus.ESCALATED
                && escalationRepository.hasActiveEscalation(planId)) {
            throw new BusinessException("存在未处理的升级记录，护士不能直接关闭计划，请先由医生处理升级");
        }

        plan.setStatus(PlanStatus.CLOSED);
        plan.setRemarks(plan.getRemarks() == null ? closeReason : plan.getRemarks() + "; " + closeReason);
        log.info("Plan {} closed by nurse with reason: {}", planId, closeReason);
    }

    @Transactional(readOnly = true)
    public EscalationHistory getEscalationById(Long id) {
        return escalationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escalation history", id));
    }

    @Transactional(readOnly = true)
    public List<EscalationHistory> getEscalationsByPlanId(Long planId) {
        return escalationRepository.findByPlanIdOrderByCreatedAtDesc(planId);
    }

    @Transactional(readOnly = true)
    public Optional<EscalationHistory> getActiveEscalation(Long planId) {
        return escalationRepository.findActiveEscalation(planId);
    }

    @Transactional(readOnly = true)
    public List<EscalationHistory> getEscalationsForDoctor(String doctorName) {
        return escalationRepository.findByToDoctorAndResolvedFalseOrderByCreatedAtDesc(doctorName);
    }

    @Transactional(readOnly = true)
    public List<EscalationHistory> getAllEscalations() {
        return escalationRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public boolean hasActiveEscalation(Long planId) {
        return escalationRepository.hasActiveEscalation(planId);
    }

    @Transactional(readOnly = true)
    public List<EscalationHistory> getEscalationHistoryForPlan(Long planId) {
        return escalationRepository.findAllEscalationHistoryForPlan(planId);
    }
}
