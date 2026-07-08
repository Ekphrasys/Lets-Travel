package com.travel.travel.controller;

import com.travel.travel.dto.TravelerProfileResponse;
import com.travel.travel.service.TravelerProfileService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/travelers")
public class TravelerController {

    private final TravelerProfileService travelerProfileService;

    public TravelerController(TravelerProfileService travelerProfileService) {
        this.travelerProfileService = travelerProfileService;
    }

    @GetMapping("/{travelerId}/profile")
    @PreAuthorize("hasRole('ADMIN') or #travelerId.toString() == authentication.name")
    public TravelerProfileResponse getProfile(@PathVariable UUID travelerId) {
        return travelerProfileService.getProfile(travelerId);
    }
}
