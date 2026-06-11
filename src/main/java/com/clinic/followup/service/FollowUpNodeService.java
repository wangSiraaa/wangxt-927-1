package com.clinic.followup.service;

import com.clinic.followup.entity.FollowUpNode;
import com.clinic.followup.entity.FollowUpPlan;
import com.clinic.followup.exception.ResourceNotFoundException;
import com.clinic.followup.repository.FollowUpNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FollowUpNodeService {

    private static final Logger log = LoggerFactory.getLogger(FollowUpNodeService.class);

    private final FollowUpNodeRepository nodeRepository;

    public FollowUpNodeService(FollowUpNodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    @Transactional
    public FollowUpNode createNode(FollowUpPlan plan, LocalDate followUpDate,
                                   int nodeOrder, String description) {
        FollowUpNode node = new FollowUpNode();
        node.setPlan(plan);
        node.setFollowUpDate(followUpDate);
        node.setNodeOrder(nodeOrder);
        node.setDescription(description);
        node.setCompleted(false);
        node.setCancelled(false);
        return nodeRepository.save(node);
    }

    @Transactional(readOnly = true)
    public List<FollowUpNode> getNodesByPlanId(Long planId) {
        return nodeRepository.findByPlanIdOrderByNodeOrderAsc(planId);
    }

    @Transactional(readOnly = true)
    public List<FollowUpNode> getPendingNodesByPlanId(Long planId) {
        return nodeRepository.findByPlanIdAndCompletedFalseAndCancelledFalseOrderByFollowUpDateAsc(planId);
    }

    @Transactional(readOnly = true)
    public Optional<FollowUpNode> getNextPendingNode(Long planId) {
        return nodeRepository.findNextPendingNode(planId);
    }

    @Transactional(readOnly = true)
    public FollowUpNode getNodeById(Long id) {
        return nodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up node", id));
    }

    @Transactional
    public FollowUpNode completeNode(Long nodeId) {
        FollowUpNode node = getNodeById(nodeId);
        node.setCompleted(true);
        node.setCompletedAt(LocalDateTime.now());
        log.info("Follow-up node {} completed", nodeId);
        return nodeRepository.save(node);
    }

    @Transactional
    public void cancelNode(Long nodeId) {
        FollowUpNode node = getNodeById(nodeId);
        if (!node.isCompleted()) {
            node.setCancelled(true);
            log.info("Follow-up node {} cancelled", nodeId);
            nodeRepository.save(node);
        }
    }

    @Transactional
    public void cancelAllPendingNodes(FollowUpPlan plan) {
        List<FollowUpNode> pendingNodes = getPendingNodesByPlanId(plan.getId());
        for (FollowUpNode node : pendingNodes) {
            node.setCancelled(true);
        }
        nodeRepository.saveAll(pendingNodes);
        log.info("Cancelled {} pending nodes for plan {}", pendingNodes.size(), plan.getId());
    }

    @Transactional
    public List<FollowUpNode> getNodesDueBetween(LocalDate startDate, LocalDate endDate) {
        return nodeRepository.findNodesDueBetween(startDate, endDate);
    }

    @Transactional
    public List<FollowUpNode> getNodesDueToday() {
        LocalDate today = LocalDate.now();
        return getNodesDueBetween(today, today);
    }

    @Transactional
    public List<FollowUpNode> getNodesDueThisWeek() {
        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.plusDays(7);
        return getNodesDueBetween(today, endOfWeek);
    }

    @Transactional(readOnly = true)
    public boolean hasNodeOnDate(Long planId, LocalDate date) {
        return nodeRepository.existsByPlanIdAndFollowUpDate(planId, date);
    }

    @Transactional
    public FollowUpNode rescheduleNode(Long nodeId, LocalDate newDate) {
        FollowUpNode node = getNodeById(nodeId);
        if (node.isCompleted()) {
            throw new IllegalStateException("Cannot reschedule a completed node");
        }
        if (newDate.isBefore(node.getPlan().getDischargeDate())) {
            throw new IllegalArgumentException("复诊日期不能早于出院日期");
        }
        node.setFollowUpDate(newDate);
        log.info("Follow-up node {} rescheduled to {}", nodeId, newDate);
        return nodeRepository.save(node);
    }
}
