package com.opspilot.simulation;

import com.opspilot.incident.*;
import com.opspilot.registry.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {
    private final ServiceRepository services;
    private final IncidentRepository incidents;

    public SimulationController(ServiceRepository services, IncidentRepository incidents) {
        this.services = services;
        this.incidents = incidents;
    }

    @PostMapping("/failures")
    @Transactional
    public ResponseEntity<?> injectFailure(@Valid @RequestBody FailureRequest request) {
        var service = services.findByName(request.serviceName()).orElse(null);
        if (service == null) return ResponseEntity.badRequest().body(Map.of("error", "Unknown service", "serviceName", request.serviceName()));
        try {
            service.applyFailure(request.failureType());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        IncidentSeverity severity = request.failureType().equals("SERVICE_DOWN") ? IncidentSeverity.CRITICAL :
            request.failureType().equals("DEPLOYMENT_REGRESSION") || request.failureType().equals("HIGH_ERROR_RATE") ? IncidentSeverity.HIGH : IncidentSeverity.MEDIUM;
        var incident = incidents.save(new Incident(service.getName(), severity, IncidentStatus.DETECTED,
            "Simulated " + request.failureType() + " detected for " + service.getName(),
            "failureType=" + request.failureType() + "; status=" + service.getStatus() +
            "; errorRatePercent=" + service.getErrorRatePercent() +
            "; p95LatencyMs=" + service.getP95LatencyMs() +
            "; databaseConnectionsPercent=" + service.getDatabaseConnectionsPercent() +
            "; kafkaConsumerLag=" + service.getKafkaConsumerLag()));
        return ResponseEntity.ok(Map.of("service", service, "incident", incident));
    }

    @PostMapping("/recoveries")
    @Transactional
    public ResponseEntity<?> recover(@RequestParam String serviceName) {
        var service = services.findByName(serviceName).orElse(null);
        if (service == null) return ResponseEntity.badRequest().body(Map.of("error", "Unknown service", "serviceName", serviceName));
        service.recover();
        incidents.findAllByOrderByCreatedAtDesc().stream()
            .filter(i -> i.getServiceName().equals(serviceName) && i.getStatus() != IncidentStatus.RESOLVED && i.getStatus() != IncidentStatus.CLOSED)
            .findFirst().ifPresent(Incident::resolve);
        return ResponseEntity.ok(Map.of("message", "Simulated recovery applied", "service", service));
    }
}
