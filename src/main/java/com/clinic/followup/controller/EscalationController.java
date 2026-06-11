package com.clinic.followup.controller;

import com.clinic.followup.dto.ApiResponse;
import com.clinic.followup.dto.EscalateRequest;
import com.clinic.followup.dto.ResolveEscalationRequest;
import com.clinic.followup.entity.EscalationHistory;
import com.clinic.followup.service.EscalationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/escalations")
public class EscalationController {

    private final EscalationService escalationService;

    public EscalationController(EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    @PostMapping
    public ApiResponse<EscalationHistory> createEscalation(@Valid @RequestBody EscalateRequest request) {
        EscalationHistory escalation = escalationService.createEscalation(request);
        return ApiResponse.success("升级成功", escalation);
    }

    @PostMapping("/resolve")
    public ApiResponse<EscalationHistory> resolveEscalation(@Valid @RequestBody ResolveEscalationRequest request) {
        EscalationHistory escalation = escalationService.resolveEscalation(request);
        return ApiResponse.success("处理完成", escalation);
    }

    @GetMapping("/{id}")
    public ApiResponse<EscalationHistory> getEscalationById(@PathVariable Long id) {
        EscalationHistory escalation = escalationService.getEscalationById(id);
        return ApiResponse.success(escalation);
    }

    @GetMapping
    public ApiResponse<List<EscalationHistory>> getAllEscalations() {
        List<EscalationHistory> escalations = escalationService.getAllEscalations();
        return ApiResponse.success(escalations);
    }

    @GetMapping("/plan/{planId}")
    public ApiResponse<List<EscalationHistory>> getEscalationsByPlan(@PathVariable Long planId) {
        List<EscalationHistory> escalations = escalationService.getEscalationsByPlanId(planId);
        return ApiResponse.success(escalations);
    }

    @GetMapping("/plan/{planId}/active")
    public ApiResponse<EscalationHistory> getActiveEscalation(@PathVariable Long planId) {
        Optional<EscalationHistory> escalation = escalationService.getActiveEscalation(planId);
        return escalation.map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.error(404, "没有待处理的升级记录"));
    }

    @GetMapping("/plan/{planId}/history")
    public ApiResponse<List<EscalationHistory>> getEscalationHistory(@PathVariable Long planId) {
        List<EscalationHistory> history = escalationService.getEscalationHistoryForPlan(planId);
        return ApiResponse.success(history);
    }

    @GetMapping("/doctor/{doctorName}")
    public ApiResponse<List<EscalationHistory>> getEscalationsForDoctor(@PathVariable String doctorName) {
        List<EscalationHistory> escalations = escalationService.getEscalationsForDoctor(doctorName);
        return ApiResponse.success(escalations);
    }

    @PostMapping("/close-plan/{planId}")
    public ApiResponse<Void> closePlanByNurse(@PathVariable Long planId,
                                               @RequestParam String reason) {
        escalationService.closePlanByNurse(planId, reason);
        return ApiResponse.success("计划已关闭", null);
    }
}
