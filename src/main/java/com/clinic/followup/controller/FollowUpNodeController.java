package com.clinic.followup.controller;

import com.clinic.followup.dto.ApiResponse;
import com.clinic.followup.entity.FollowUpNode;
import com.clinic.followup.service.FollowUpNodeService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/nodes")
public class FollowUpNodeController {

    private final FollowUpNodeService nodeService;

    public FollowUpNodeController(FollowUpNodeService nodeService) {
        this.nodeService = nodeService;
    }

    @GetMapping("/{id}")
    public ApiResponse<FollowUpNode> getNodeById(@PathVariable Long id) {
        FollowUpNode node = nodeService.getNodeById(id);
        return ApiResponse.success(node);
    }

    @GetMapping("/plan/{planId}")
    public ApiResponse<List<FollowUpNode>> getNodesByPlan(@PathVariable Long planId) {
        List<FollowUpNode> nodes = nodeService.getNodesByPlanId(planId);
        return ApiResponse.success(nodes);
    }

    @GetMapping("/plan/{planId}/pending")
    public ApiResponse<List<FollowUpNode>> getPendingNodes(@PathVariable Long planId) {
        List<FollowUpNode> nodes = nodeService.getPendingNodesByPlanId(planId);
        return ApiResponse.success(nodes);
    }

    @GetMapping("/plan/{planId}/next")
    public ApiResponse<FollowUpNode> getNextPendingNode(@PathVariable Long planId) {
        Optional<FollowUpNode> node = nodeService.getNextPendingNode(planId);
        return node.map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.error(404, "没有待处理的随访节点"));
    }

    @GetMapping("/due/today")
    public ApiResponse<List<FollowUpNode>> getNodesDueToday() {
        List<FollowUpNode> nodes = nodeService.getNodesDueToday();
        return ApiResponse.success(nodes);
    }

    @GetMapping("/due/week")
    public ApiResponse<List<FollowUpNode>> getNodesDueThisWeek() {
        List<FollowUpNode> nodes = nodeService.getNodesDueThisWeek();
        return ApiResponse.success(nodes);
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<FollowUpNode> completeNode(@PathVariable Long id) {
        FollowUpNode node = nodeService.completeNode(id);
        return ApiResponse.success("标记完成", node);
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancelNode(@PathVariable Long id) {
        nodeService.cancelNode(id);
        return ApiResponse.success("已取消", null);
    }

    @PutMapping("/{id}/reschedule")
    public ApiResponse<FollowUpNode> rescheduleNode(@PathVariable Long id,
                                                    @RequestParam LocalDate newDate) {
        FollowUpNode node = nodeService.rescheduleNode(id, newDate);
        return ApiResponse.success("已改期", node);
    }
}
