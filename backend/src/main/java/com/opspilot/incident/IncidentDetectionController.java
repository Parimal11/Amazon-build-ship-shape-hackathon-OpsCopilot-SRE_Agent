package com.opspilot.incident;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
public class IncidentDetectionController {

    private final IncidentDetectionService detectionService;

    public IncidentDetectionController(
            IncidentDetectionService detectionService) {
        this.detectionService = detectionService;
    }

    @PostMapping("/detect")
    public Map<String, Object> detect() {
        List<Incident> detected = detectionService.detectIncidents();

        return Map.of(
                "createdCount", detected.size(),
                "incidents", detected
        );
    }
}