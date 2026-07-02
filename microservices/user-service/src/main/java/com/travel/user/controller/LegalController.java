package com.travel.user.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/legal")
public class LegalController {

    @GetMapping(value = "/privacy", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PrivacyPolicyResponse> privacy() {
        PrivacyPolicyResponse body = new PrivacyPolicyResponse(
                "1.0",
                "2025-06-01",
                "Lets Travel",
                "e5e5e5e5-e5e5-e5e5-e5e5-e5e5e5e5e5e5",
                "Lets Travel collecte uniquement les données nécessaires au service : "
                        + "identité, réservations, paiements et avis. "
                        + "Vos données sont conservées le temps nécessaire aux fins du service "
                        + "et aux obligations légales. "
                        + "Vous disposez d'un droit d'accès, de rectification, d'export et de suppression.",
                true
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }

    public record PrivacyPolicyResponse(
            String version,
            String effectiveDate,
            String organization,
            String dpoContact,
            String summary,
            boolean userControl
    ) {
    }
}
