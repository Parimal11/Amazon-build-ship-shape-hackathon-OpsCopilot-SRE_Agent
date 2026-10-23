package com.opspilot.incident;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
public class IncidentInvestigationController {

    private final IncidentRepository incidentRepository;
    private final AiInvestigationService aiInvestigationService;

    public IncidentInvestigationController(
            IncidentRepository incidentRepository,
            AiInvestigationService aiInvestigationService) {
        this.incidentRepository = incidentRepository;
        this.aiInvestigationService = aiInvestigationService;
    }

    @PostMapping("/{id}/investigate")
    public Map<String, Object> investigate(@PathVariable Long id) {

        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Incident not found: " + id
                ));

        JsonNode analysis = aiInvestigationService.investigate(incident);

        return Map.of(
                "incidentId", incident.getId(),
                "serviceName", incident.getServiceName(),
                "analysis", analysis,
                "requiresHumanApproval", true
        );
    }
}