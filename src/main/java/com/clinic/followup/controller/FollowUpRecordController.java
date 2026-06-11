package com.clinic.followup.controller;

import com.clinic.followup.dto.ApiResponse;
import com.clinic.followup.dto.CreateRecordRequest;
import com.clinic.followup.entity.FollowUpRecord;
import com.clinic.followup.service.FollowUpRecordService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/records")
public class FollowUpRecordController {

    private final FollowUpRecordService recordService;

    public FollowUpRecordController(FollowUpRecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping
    public ApiResponse<FollowUpRecord> createRecord(@Valid @RequestBody CreateRecordRequest request) {
        FollowUpRecord record = recordService.createRecord(request);
        return ApiResponse.success("记录成功", record);
    }

    @GetMapping("/{id}")
    public ApiResponse<FollowUpRecord> getRecordById(@PathVariable Long id) {
        FollowUpRecord record = recordService.getRecordById(id);
        return ApiResponse.success(record);
    }

    @GetMapping
    public ApiResponse<List<FollowUpRecord>> getAllRecords() {
        List<FollowUpRecord> records = recordService.getAllRecords();
        return ApiResponse.success(records);
    }

    @GetMapping("/plan/{planId}")
    public ApiResponse<List<FollowUpRecord>> getRecordsByPlan(@PathVariable Long planId) {
        List<FollowUpRecord> records = recordService.getRecordsByPlanId(planId);
        return ApiResponse.success(records);
    }

    @GetMapping("/plan/{planId}/latest")
    public ApiResponse<FollowUpRecord> getLatestRecord(@PathVariable Long planId) {
        Optional<FollowUpRecord> record = recordService.getLatestRecordByPlanId(planId);
        return record.map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.error(404, "没有找到随访记录"));
    }

    @GetMapping("/reminders/today")
    public ApiResponse<List<FollowUpRecord>> getTodayReminders() {
        List<FollowUpRecord> records = recordService.getTodayReminders();
        return ApiResponse.success(records);
    }

    @GetMapping("/reminders/week")
    public ApiResponse<List<FollowUpRecord>> getThisWeekReminders() {
        List<FollowUpRecord> records = recordService.getThisWeekReminders();
        return ApiResponse.success(records);
    }

    @GetMapping("/reminders")
    public ApiResponse<List<FollowUpRecord>> getRemindersBetween(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<FollowUpRecord> records = recordService.getRemindersBetween(startDate, endDate);
        return ApiResponse.success(records);
    }
}
