package com.clinic.followup.service;

import com.clinic.followup.dto.PatientFollowUpVO;
import com.clinic.followup.entity.FollowUpNode;
import com.clinic.followup.entity.FollowUpPlan;
import com.clinic.followup.entity.FollowUpRecord;
import com.clinic.followup.enums.TransferStatus;
import com.clinic.followup.exception.ResourceNotFoundException;
import com.clinic.followup.repository.FollowUpPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PatientQueryService {

    private static final Logger log = LoggerFactory.getLogger(PatientQueryService.class);

    private final FollowUpPlanRepository planRepository;
    private final FollowUpNodeService nodeService;
    private final FollowUpRecordService recordService;

    public PatientQueryService(FollowUpPlanRepository planRepository,
                               FollowUpNodeService nodeService,
                               FollowUpRecordService recordService) {
        this.planRepository = planRepository;
        this.nodeService = nodeService;
        this.recordService = recordService;
    }

    @Transactional(readOnly = true)
    public PatientFollowUpVO getPatientFollowUpInfo(String patientIdCard) {
        List<FollowUpPlan> plans = planRepository.findActivePlansByPatientIdCard(patientIdCard);

        if (plans.isEmpty()) {
            throw new ResourceNotFoundException("No active follow-up plan found for patient: " + patientIdCard);
        }

        FollowUpPlan activePlan = plans.stream()
                .filter(p -> p.getTransferStatus() == TransferStatus.NOT_TRANSFERRED)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No active follow-up plan found for patient: " + patientIdCard));

        return convertToVO(activePlan);
    }

    @Transactional(readOnly = true)
    public List<PatientFollowUpVO> getPatientHistory(String patientIdCard) {
        List<FollowUpPlan> plans = planRepository.findByPatientIdCard(patientIdCard);
        List<PatientFollowUpVO> result = new ArrayList<>();

        for (FollowUpPlan plan : plans) {
            result.add(convertToVO(plan));
        }

        result.sort(Comparator.comparing(PatientFollowUpVO::getNextFollowUpDate,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return result;
    }

    private PatientFollowUpVO convertToVO(FollowUpPlan plan) {
        PatientFollowUpVO vo = new PatientFollowUpVO();
        vo.setPlanId(plan.getId());
        vo.setPatientName(plan.getPatientName());
        vo.setDiseaseType(plan.getDiseaseType());
        vo.setRiskLevel(plan.getRiskLevel().getDescription());
        vo.setDischargeDate(plan.getDischargeDate());
        vo.setAttendingDoctor(plan.getAttendingDoctor());
        vo.setAssignedNurse(plan.getAssignedNurse());

        Optional<FollowUpNode> nextNode = nodeService.getNextPendingNode(plan.getId());
        if (nextNode.isPresent()) {
            FollowUpNode node = nextNode.get();
            vo.setNextFollowUpDate(node.getFollowUpDate());
            vo.setNextFollowUpNote(node.getDescription());
        } else {
            Optional<FollowUpRecord> latestRecord = recordService.getLatestRecordByPlanId(plan.getId());
            if (latestRecord.isPresent() && latestRecord.get().getNextReminderDate() != null) {
                FollowUpRecord record = latestRecord.get();
                vo.setNextFollowUpDate(record.getNextReminderDate());
                vo.setNextFollowUpNote(record.getNextReminderNote());
                vo.setNeedExamReport(record.isNeedExamReport());
                vo.setExamReportNote(record.getExamReportNote());
            }
        }

        Optional<FollowUpRecord> lastSuccessRecord = recordService.getLastSuccessfulRecord(plan.getId());
        if (lastSuccessRecord.isPresent()) {
            FollowUpRecord record = lastSuccessRecord.get();
            vo.setLastContactTime(record.getCreatedAt());
            vo.setLastContactResult(record.getCallResult().getDescription());
            if (vo.getNextFollowUpDate() == null) {
                vo.setNeedExamReport(record.isNeedExamReport());
                vo.setExamReportNote(record.getExamReportNote());
            }
        }

        return vo;
    }

    @Transactional(readOnly = true)
    public LocalDate getNextFollowUpDate(String patientIdCard) {
        PatientFollowUpVO info = getPatientFollowUpInfo(patientIdCard);
        return info.getNextFollowUpDate();
    }

    @Transactional(readOnly = true)
    public boolean needsExamReport(String patientIdCard) {
        PatientFollowUpVO info = getPatientFollowUpInfo(patientIdCard);
        return info.isNeedExamReport();
    }

    @Transactional(readOnly = true)
    public String getExamReportNote(String patientIdCard) {
        PatientFollowUpVO info = getPatientFollowUpInfo(patientIdCard);
        return info.getExamReportNote();
    }

    @Transactional(readOnly = true)
    public List<FollowUpRecord> getPatientFollowUpHistory(String patientIdCard) {
        List<FollowUpPlan> plans = planRepository.findByPatientIdCard(patientIdCard);
        List<FollowUpRecord> allRecords = new ArrayList<>();

        for (FollowUpPlan plan : plans) {
            allRecords.addAll(recordService.getRecordsByPlanId(plan.getId()));
        }

        allRecords.sort(Comparator.comparing(FollowUpRecord::getCreatedAt).reversed());
        return allRecords;
    }
}
