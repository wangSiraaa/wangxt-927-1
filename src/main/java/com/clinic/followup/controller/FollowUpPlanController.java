package com.clinic.followup.controller;

import com.clinic.followup.dto.ApiResponse;
import com.clinic.followup.dto.CreatePlanRequest;
import com.clinic.followup.dto.UpdatePlanRequest;
import com.clinic.followup.entity.FollowUpPlan;
import com.clinic.followup.service.FollowUpPlanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/plans")
public class FollowUpPlanController {

    private final FollowUpPlanService planService;

    public FollowUpPlanController(FollowUpPlanService planService) {
        this.planService = planService;
    }

    @PostMapping
    public ApiResponse<FollowUpPlan> createPlan(@Valid @RequestBody CreatePlanRequest request) {
        FollowUpPlan plan = planService.createPlan(request);
        return ApiResponse.success("创建成功", plan);
    }

    @PutMapping("/{id}")
    public ApiResponse<FollowUpPlan> updatePlan(@PathVariable Long id,
                                                @Valid @RequestBody UpdatePlanRequest request) {
        FollowUpPlan plan = planService.updatePlan(id, request);
        return ApiResponse.success("更新成功", plan);
    }

    @GetMapping("/{id}")
    public ApiResponse<FollowUpPlan> getPlanById(@PathVariable Long id) {
        FollowUpPlan plan = planService.getPlanById(id);
        return ApiResponse.success(plan);
    }

    @GetMapping
    public ApiResponse<List<FollowUpPlan>> getAllPlans() {
        List<FollowUpPlan> plans = planService.getAllPlans();
        return ApiResponse.success(plans);
    }

    @GetMapping("/patient/{patientIdCard}")
    public ApiResponse<List<FollowUpPlan>> getPlansByPatient(@PathVariable String patientIdCard) {
        List<FollowUpPlan> plans = planService.getPlansByPatientIdCard(patientIdCard);
        return ApiResponse.success(plans);
    }

    @GetMapping("/nurse/{nurseName}")
    public ApiResponse<List<FollowUpPlan>> getPlansByNurse(@PathVariable String nurseName) {
        List<FollowUpPlan> plans = planService.getPlansByNurse(nurseName);
        return ApiResponse.success(plans);
    }

    @GetMapping("/doctor/{doctorName}")
    public ApiResponse<List<FollowUpPlan>> getPlansByDoctor(@PathVariable String doctorName) {
        List<FollowUpPlan> plans = planService.getPlansByDoctor(doctorName);
        return ApiResponse.success(plans);
    }

    @GetMapping("/active")
    public ApiResponse<List<FollowUpPlan>> getActivePlans() {
        List<FollowUpPlan> plans = planService.getActivePlans();
        return ApiResponse.success(plans);
    }

    @GetMapping("/escalated")
    public ApiResponse<List<FollowUpPlan>> getEscalatedPlans() {
        List<FollowUpPlan> plans = planService.getEscalatedPlans();
        return ApiResponse.success(plans);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return ApiResponse.success("删除成功", null);
    }
}
