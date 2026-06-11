package com.clinic.followup.service;

import com.clinic.followup.config.FollowUpConfig;
import com.clinic.followup.dto.CreateRecordRequest;
import com.clinic.followup.entity.FollowUpNode;
import com.clinic.followup.entity.FollowUpPlan;
import com.clinic.followup.entity.FollowUpRecord;
import com.clinic.followup.enums.CallResult;
import com.clinic.followup.enums.PlanStatus;
import com.clinic.followup.enums.TransferStatus;
import com.clinic.followup.exception.BusinessException;
import com.clinic.followup.exception.ResourceNotFoundException;
import com.clinic.followup.repository.FollowUpRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class FollowUpRecordService {

    private static final Logger log = LoggerFactory.getLogger(FollowUpRecordService.class);

    private final FollowUpRecordRepository recordRepository;
    private final FollowUpPlanService planService;
    private final FollowUpNodeService nodeService;
    private final FollowUpConfig config;

    public FollowUpRecordService(FollowUpRecordRepository recordRepository,
                                 FollowUpPlanService planService,
                                 FollowUpNodeService nodeService,
                                 FollowUpConfig config) {
        this.recordRepository = recordRepository;
        this.planService = planService;
        this.nodeService = nodeService;
        this.config = config;
    }

    @Transactional
    public FollowUpRecord createRecord(CreateRecordRequest request) {
        FollowUpPlan plan = planService.getPlanById(request.getPlanId());

        if (plan.getTransferStatus() == TransferStatus.TRANSFERRED) {
            throw new BusinessException("已转院患者不能创建新的随访记录");
        }

        if (request.getNextReminderDate() != null) {
            planService.validateFollowUpDate(plan, request.getNextReminderDate());
        }

        FollowUpRecord record = new FollowUpRecord();
        record.setPlan(plan);
        record.setCallResult(request.getCallResult());
        record.setNoAnswerReason(request.getNoAnswerReason());
        record.setConversationContent(request.getConversationContent());
        record.setNeedExamReport(request.isNeedExamReport());
        record.setExamReportNote(request.getExamReportNote());
        record.setOperatorName(request.getOperatorName());

        if (request.getNextReminderDate() != null) {
            record.setNextReminderDate(request.getNextReminderDate());
        } else if (request.getCallResult() != CallResult.CONNECTED) {
            LocalDate defaultReminder = LocalDate.now().plusDays(config.getReminder().getDefaultDays());
            record.setNextReminderDate(defaultReminder);
            record.setNextReminderNote("系统自动生成提醒");
        }
        if (request.getNextReminderNote() != null) {
            record.setNextReminderNote(request.getNextReminderNote());
        }

        if (request.getNodeId() != null) {
            FollowUpNode node = nodeService.getNodeById(request.getNodeId());
            if (!node.getPlan().getId().equals(plan.getId())) {
                throw new BusinessException("随访节点不属于该计划");
            }
            record.setNode(node);
            if (request.getCallResult() == CallResult.CONNECTED) {
                nodeService.completeNode(node.getId());
            }
        }

        updatePlanStatusAfterRecord(plan, request.getCallResult());

        return recordRepository.save(record);
    }

    private void updatePlanStatusAfterRecord(FollowUpPlan plan, CallResult callResult) {
        if (callResult == CallResult.CONNECTED) {
            planService.resetConsecutiveMissed(plan);
            if (plan.getStatus() == PlanStatus.ESCALATED) {
                log.info("Patient {} reconnected after escalation, resuming follow-up but keeping escalation history",
                        plan.getPatientName());
            }
        } else {
            planService.incrementConsecutiveMissed(plan);
        }
    }

    @Transactional(readOnly = true)
    public List<FollowUpRecord> getRecordsByPlanId(Long planId) {
        return recordRepository.findByPlanIdOrderByCreatedAtDesc(planId);
    }

    @Transactional(readOnly = true)
    public Optional<FollowUpRecord> getLatestRecordByPlanId(Long planId) {
        return recordRepository.findLatestRecordByPlanId(planId);
    }

    @Transactional(readOnly = true)
    public List<FollowUpRecord> getAllRecords() {
        return recordRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public FollowUpRecord getRecordById(Long id) {
        return recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up record", id));
    }

    @Transactional(readOnly = true)
    public List<FollowUpRecord> getRemindersBetween(LocalDate startDate, LocalDate endDate) {
        return recordRepository.findRecordsWithRemindersBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<FollowUpRecord> getTodayReminders() {
        LocalDate today = LocalDate.now();
        return getRemindersBetween(today, today);
    }

    @Transactional(readOnly = true)
    public List<FollowUpRecord> getThisWeekReminders() {
        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.plusDays(7);
        return getRemindersBetween(today, endOfWeek);
    }

    @Transactional(readOnly = true)
    public long countMissedCallsSince(Long planId, LocalDate since) {
        return recordRepository.countFailedCallsSince(planId,
                List.of(CallResult.NO_ANSWER, CallResult.BUSY,
                        CallResult.WRONG_NUMBER, CallResult.REFUSED),
                since.atStartOfDay());
    }

    @Transactional(readOnly = true)
    public Optional<FollowUpRecord> getLastSuccessfulRecord(Long planId) {
        return recordRepository.findLastSuccessfulRecord(planId);
    }
}
