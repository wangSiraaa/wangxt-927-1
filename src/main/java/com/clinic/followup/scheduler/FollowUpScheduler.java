package com.clinic.followup.scheduler;

import com.clinic.followup.config.FollowUpConfig;
import com.clinic.followup.entity.FollowUpPlan;
import com.clinic.followup.enums.PlanStatus;
import com.clinic.followup.enums.TransferStatus;
import com.clinic.followup.repository.FollowUpPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class FollowUpScheduler {

    private static final Logger log = LoggerFactory.getLogger(FollowUpScheduler.class);

    private final FollowUpPlanRepository planRepository;
    private final FollowUpConfig config;

    public FollowUpScheduler(FollowUpPlanRepository planRepository, FollowUpConfig config) {
        this.planRepository = planRepository;
        this.config = config;
    }

    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void checkEscalationThreshold() {
        log.info("Starting scheduled escalation threshold check...");

        List<FollowUpPlan> plans = planRepository.findPlansNeedingEscalation(
                config.getEscalation().getThreshold());

        int escalatedCount = 0;
        for (FollowUpPlan plan : plans) {
            if (plan.getStatus() == PlanStatus.ACTIVE
                    && plan.getTransferStatus() == TransferStatus.NOT_TRANSFERRED) {
                plan.setStatus(PlanStatus.ESCALATED);
                escalatedCount++;
                log.warn("Plan {} automatically escalated - consecutive missed: {}, threshold: {}",
                        plan.getId(), plan.getConsecutiveMissed(),
                        config.getEscalation().getThreshold());
            }
        }

        if (escalatedCount > 0) {
            planRepository.saveAll(plans);
        }

        log.info("Escalation threshold check completed. {} plans escalated.", escalatedCount);
    }

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void generateDailyReminders() {
        log.info("Starting daily reminder generation...");

        long activePlansCount = planRepository.findByStatusAndTransferStatus(
                PlanStatus.ACTIVE, TransferStatus.NOT_TRANSFERRED).size();

        log.info("Daily reminder check completed. {} active plans requiring follow-up.", activePlansCount);
    }
}
