package com.clinic.followup.service;

import com.clinic.followup.config.FollowUpConfig;
import com.clinic.followup.dto.CreatePlanRequest;
import com.clinic.followup.dto.UpdatePlanRequest;
import com.clinic.followup.entity.EscalationHistory;
import com.clinic.followup.entity.FollowUpNode;
import com.clinic.followup.entity.FollowUpPlan;
import com.clinic.followup.enums.PlanStatus;
import com.clinic.followup.enums.RiskLevel;
import com.clinic.followup.enums.TransferStatus;
import com.clinic.followup.exception.BusinessException;
import com.clinic.followup.exception.ResourceNotFoundException;
import com.clinic.followup.repository.FollowUpPlanRepository;
import com.clinic.followup.repository.EscalationHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FollowUpPlanService {

    private static final Logger log = LoggerFactory.getLogger(FollowUpPlanService.class);

    private final FollowUpPlanRepository planRepository;
    private final FollowUpNodeService nodeService;
    private final EscalationHistoryRepository escalationRepository;
    private final FollowUpConfig config;

    public FollowUpPlanService(FollowUpPlanRepository planRepository,
                               FollowUpNodeService nodeService,
                               EscalationHistoryRepository escalationRepository,
                               FollowUpConfig config) {
        this.planRepository = planRepository;
        this.nodeService = nodeService;
        this.escalationRepository = escalationRepository;
        this.config = config;
    }

    @Transactional
    public FollowUpPlan createPlan(CreatePlanRequest request) {
        validateCreateRequest(request);

        FollowUpPlan plan = new FollowUpPlan();
        plan.setPatientName(request.getPatientName());
        plan.setPatientIdCard(request.getPatientIdCard());
        plan.setPatientPhone(request.getPatientPhone());
        plan.setDischargeDate(request.getDischargeDate());
        plan.setDiseaseType(request.getDiseaseType());
        plan.setRiskLevel(request.getRiskLevel());
        plan.setTransferStatus(request.getTransferStatus());
        plan.setAttendingDoctor(request.getAttendingDoctor());
        plan.setAssignedNurse(request.getAssignedNurse());
        plan.setRemarks(request.getRemarks());
        plan.setStatus(PlanStatus.ACTIVE);
        plan.setConsecutiveMissed(0);

        plan = planRepository.save(plan);

        if (request.getTransferStatus() == TransferStatus.NOT_TRANSFERRED) {
            List<FollowUpNode> nodes = generateFollowUpNodes(plan);
            plan.setNodes(nodes);
            log.info("Generated {} follow-up nodes for plan {} of patient {}",
                    nodes.size(), plan.getId(), plan.getPatientName());
        } else {
            log.info("Plan {} for patient {} is transferred, no follow-up nodes generated",
                    plan.getId(), plan.getPatientName());
        }

        return plan;
    }

    private void validateCreateRequest(CreatePlanRequest request) {
        if (request.getDischargeDate().isAfter(LocalDate.now())) {
            throw new BusinessException("出院日期不能晚于当前日期");
        }

        if (request.getTransferStatus() == TransferStatus.NOT_TRANSFERRED
                && planRepository.hasActivePlanForPatient(request.getPatientIdCard())) {
            log.warn("Patient {} already has an active follow-up plan", request.getPatientIdCard());
        }
    }

    private List<FollowUpNode> generateFollowUpNodes(FollowUpPlan plan) {
        List<FollowUpNode> nodes = new ArrayList<>();
        int intervalDays = getIntervalByRiskLevel(plan.getRiskLevel());
        int nodeOrder = 1;

        LocalDate nextDate = plan.getDischargeDate().plusDays(intervalDays);
        for (int i = 0; i < 4; i++) {
            if (nextDate.isBefore(plan.getDischargeDate())) {
                throw new BusinessException("复诊日期不能早于出院日期");
            }
            FollowUpNode node = nodeService.createNode(plan, nextDate, nodeOrder,
                    String.format("第%d次随访", nodeOrder));
            nodes.add(node);
            nodeOrder++;
            nextDate = nextDate.plusDays(intervalDays);
        }

        return nodes;
    }

    private int getIntervalByRiskLevel(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case HIGH -> config.getRisk().getHighInterval();
            case MEDIUM -> config.getRisk().getMediumInterval();
            case LOW -> config.getRisk().getLowInterval();
        };
    }

    @Transactional
    public FollowUpPlan updatePlan(Long id, UpdatePlanRequest request) {
        FollowUpPlan plan = getPlanById(id);

        boolean wasTransferred = plan.getTransferStatus() == TransferStatus.TRANSFERRED;
        boolean becomingTransferred = request.getTransferStatus() == TransferStatus.TRANSFERRED;

        if (request.getDischargeDate() != null) {
            if (request.getDischargeDate().isAfter(LocalDate.now())) {
                throw new BusinessException("出院日期不能晚于当前日期");
            }
            plan.setDischargeDate(request.getDischargeDate());
        }

        if (request.getDiseaseType() != null) {
            plan.setDiseaseType(request.getDiseaseType());
        }
        if (request.getRiskLevel() != null) {
            plan.setRiskLevel(request.getRiskLevel());
        }
        if (request.getTransferStatus() != null) {
            if (becomingTransferred && !wasTransferred) {
                nodeService.cancelAllPendingNodes(plan);
                log.info("Plan {} is being transferred, cancelling all pending nodes", id);
            } else if (!becomingTransferred && wasTransferred) {
                throw new BusinessException("已转院的计划不能恢复为未转院状态");
            }
            plan.setTransferStatus(request.getTransferStatus());
        }
        if (request.getStatus() != null) {
            if (plan.getStatus() == PlanStatus.ESCALATED && request.getStatus() == PlanStatus.CLOSED) {
                if (escalationRepository.hasActiveEscalation(id)) {
                    throw new BusinessException("存在未处理的升级记录，护士不能直接关闭计划，请先由医生处理升级");
                }
            }
            plan.setStatus(request.getStatus());
        }
        if (request.getAttendingDoctor() != null) {
            plan.setAttendingDoctor(request.getAttendingDoctor());
        }
        if (request.getAssignedNurse() != null) {
            plan.setAssignedNurse(request.getAssignedNurse());
        }
        if (request.getRemarks() != null) {
            plan.setRemarks(request.getRemarks());
        }

        return planRepository.save(plan);
    }

    @Transactional(readOnly = true)
    public FollowUpPlan getPlanById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up plan", id));
    }

    @Transactional(readOnly = true)
    public List<FollowUpPlan> getAllPlans() {
        return planRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public List<FollowUpPlan> getPlansByPatientIdCard(String patientIdCard) {
        return planRepository.findActivePlansByPatientIdCard(patientIdCard);
    }

    @Transactional(readOnly = true)
    public List<FollowUpPlan> getPlansByNurse(String nurseName) {
        return planRepository.findByAssignedNurse(nurseName);
    }

    @Transactional(readOnly = true)
    public List<FollowUpPlan> getPlansByDoctor(String doctorName) {
        return planRepository.findByAttendingDoctor(doctorName);
    }

    @Transactional(readOnly = true)
    public List<FollowUpPlan> getActivePlans() {
        return planRepository.findByStatusAndTransferStatus(
                PlanStatus.ACTIVE, TransferStatus.NOT_TRANSFERRED);
    }

    @Transactional(readOnly = true)
    public List<FollowUpPlan> getEscalatedPlans() {
        return planRepository.findByStatus(PlanStatus.ESCALATED);
    }

    @Transactional
    public void incrementConsecutiveMissed(FollowUpPlan plan) {
        plan.setConsecutiveMissed(plan.getConsecutiveMissed() + 1);
        log.debug("Plan {} consecutive missed incremented to {}", plan.getId(), plan.getConsecutiveMissed());

        if (plan.getConsecutiveMissed() >= config.getEscalation().getThreshold()
                && plan.getStatus() == PlanStatus.ACTIVE
                && !escalationRepository.hasActiveEscalation(plan.getId())) {
            plan.setStatus(PlanStatus.ESCALATED);

            EscalationHistory escalation = new EscalationHistory();
            escalation.setPlan(plan);
            escalation.setEscalationType("CONSECUTIVE_MISSED");
            escalation.setFromRole("SYSTEM");
            escalation.setToDoctor(plan.getAttendingDoctor() != null ? plan.getAttendingDoctor() : "系统管理员");
            escalation.setReason(String.format("连续%d次未联系到患者，自动升级给医生处理", plan.getConsecutiveMissed()));
            escalation.setResolved(false);
            escalationRepository.save(escalation);

            log.info("Plan {} automatically escalated due to {} consecutive missed calls, escalation record created",
                    plan.getId(), plan.getConsecutiveMissed());
        }
        planRepository.save(plan);
    }

    @Transactional
    public void resetConsecutiveMissed(FollowUpPlan plan) {
        if (plan.getConsecutiveMissed() > 0) {
            log.debug("Plan {} consecutive missed reset from {} to 0",
                    plan.getId(), plan.getConsecutiveMissed());
            plan.setConsecutiveMissed(0);
            planRepository.save(plan);
        }
    }

    @Transactional
    public void resumeFromEscalation(FollowUpPlan plan) {
        if (plan.getStatus() == PlanStatus.ESCALATED) {
            plan.setStatus(PlanStatus.ACTIVE);
            plan.setConsecutiveMissed(0);
            planRepository.save(plan);
            log.info("Plan {} resumed from escalation to active status", plan.getId());
        }
    }

    @Transactional
    public void validateFollowUpDate(FollowUpPlan plan, LocalDate followUpDate) {
        if (followUpDate.isBefore(plan.getDischargeDate())) {
            throw new BusinessException("复诊日期不能早于出院日期");
        }
    }

    @Transactional
    public void deletePlan(Long id) {
        FollowUpPlan plan = getPlanById(id);
        plan.setStatus(PlanStatus.CANCELLED);
        nodeService.cancelAllPendingNodes(plan);
        planRepository.save(plan);
        log.info("Plan {} cancelled", id);
    }
}
