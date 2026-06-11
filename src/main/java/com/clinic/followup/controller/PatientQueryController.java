package com.clinic.followup.controller;

import com.clinic.followup.dto.ApiResponse;
import com.clinic.followup.dto.PatientFollowUpVO;
import com.clinic.followup.entity.FollowUpRecord;
import com.clinic.followup.service.PatientQueryService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/patient")
public class PatientQueryController {

    private final PatientQueryService patientQueryService;

    public PatientQueryController(PatientQueryService patientQueryService) {
        this.patientQueryService = patientQueryService;
    }

    @GetMapping("/{patientIdCard}")
    public ApiResponse<PatientFollowUpVO> getPatientFollowUpInfo(@PathVariable String patientIdCard) {
        PatientFollowUpVO vo = patientQueryService.getPatientFollowUpInfo(patientIdCard);
        return ApiResponse.success(vo);
    }

    @GetMapping("/{patientIdCard}/history")
    public ApiResponse<List<PatientFollowUpVO>> getPatientHistory(@PathVariable String patientIdCard) {
        List<PatientFollowUpVO> history = patientQueryService.getPatientHistory(patientIdCard);
        return ApiResponse.success(history);
    }

    @GetMapping("/{patientIdCard}/next-date")
    public ApiResponse<LocalDate> getNextFollowUpDate(@PathVariable String patientIdCard) {
        LocalDate nextDate = patientQueryService.getNextFollowUpDate(patientIdCard);
        return ApiResponse.success(nextDate);
    }

    @GetMapping("/{patientIdCard}/need-exam")
    public ApiResponse<Boolean> needsExamReport(@PathVariable String patientIdCard) {
        boolean needExam = patientQueryService.needsExamReport(patientIdCard);
        return ApiResponse.success(needExam);
    }

    @GetMapping("/{patientIdCard}/exam-note")
    public ApiResponse<String> getExamReportNote(@PathVariable String patientIdCard) {
        String note = patientQueryService.getExamReportNote(patientIdCard);
        return ApiResponse.success(note);
    }

    @GetMapping("/{patientIdCard}/records")
    public ApiResponse<List<FollowUpRecord>> getPatientFollowUpHistory(@PathVariable String patientIdCard) {
        List<FollowUpRecord> records = patientQueryService.getPatientFollowUpHistory(patientIdCard);
        return ApiResponse.success(records);
    }
}
