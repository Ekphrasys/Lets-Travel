package com.travel.travel.controller;

import com.travel.travel.dto.CreateReportRequest;
import com.travel.travel.dto.ManagerProfileResponse;
import com.travel.travel.dto.ReportDetailResponse;
import com.travel.travel.dto.ReportResponse;
import com.travel.travel.service.ManagerProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/managers")
public class ManagerController {

    private final ManagerProfileService managerProfileService;

    public ManagerController(ManagerProfileService managerProfileService) {
        this.managerProfileService = managerProfileService;
    }

    @GetMapping("/{managerId}/profile")
    public ManagerProfileResponse getProfile(@PathVariable UUID managerId) {
        return managerProfileService.getProfile(managerId);
    }

    @GetMapping("/my/reports")
    @PreAuthorize("hasRole('TRAVEL_MANAGER') or hasRole('ADMIN')")
    public List<ReportDetailResponse> myReports(Authentication authentication) {
        UUID managerId = UUID.fromString(authentication.getName());
        return managerProfileService.getMyReports(managerId);
    }

    @PostMapping("/{managerId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse report(@PathVariable UUID managerId,
                                 @Valid @RequestBody CreateReportRequest request,
                                 Authentication authentication) {
        UUID reporterId = UUID.fromString(authentication.getName());
        return managerProfileService.createReport(managerId, reporterId, request);
    }
}
