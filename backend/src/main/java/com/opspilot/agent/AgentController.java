
package com.opspilot.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.opspilot.incident.AiInvestigationService;
import com.opspilot.incident.Incident;
import com.opspilot.incident.IncidentDetectionService;
import com.opspilot.incident.IncidentRepository;
import com.opspilot.telemetry.PrometheusService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final IncidentRepository incidents;
    private final IncidentDetectionService detection;
    private final AiInvestigationService ai;
    private final PrometheusService prometheus;

    public AgentController(
            IncidentRepository incidents,
            IncidentDetectionService detection,
            AiInvestigationService ai,
            PrometheusService prometheus) {
        this.incidents = incidents;
        this.detection = detection;
        this.ai = ai;
        this.prometheus = prometheus;
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "").trim();

        if (message.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Please enter a message.");
        }

        String text = message.toLowerCase(Locale.ROOT);
        List<Map<String, Object>> steps = new ArrayList<>();
        Object result;

        // Check list requests before detection or investigation keywords.
        boolean listRequest =
                text.matches(".*\\b(list|show|display|view)\\b.*")
                && text.matches(".*\\b(incidents?|outages?)\\b.*");

        boolean investigateRequest =
                text.contains("investigate")
                || text.contains("root cause")
                || text.contains("analyze incident")
                || text.contains("analyse incident")
                || text.contains("why is")
                || text.contains("why did");

        boolean detectRequest =
                text.contains("detect")
                || text.contains("scan for")
                || text.contains("check for incident")
                || text.contains("run incident detection");

        boolean healthRequest =
                text.contains("health")
                || text.contains("telemetry")
                || text.contains("service status")
                || text.contains("services up");

        if (listRequest) {
            List<Incident> all =
                    incidents.findAllByOrderByCreatedAtDesc();

            result = Map.of(
                    "message", all.isEmpty()
                            ? "There are currently no recorded incidents."
                            : "Here are the recorded incidents.",
                    "incidents", all,
                    "count", all.size());

            steps.add(step("list_incidents", result));

        } else if (investigateRequest) {
            List<Incident> all =
                    incidents.findAllByOrderByCreatedAtDesc();

            Incident selected = all.stream()
                    .filter(i -> !i.getStatus().toString()
                            .equalsIgnoreCase("RESOLVED"))
                    .findFirst()
                    .orElse(all.isEmpty() ? null : all.get(0));

            if (selected == null) {
                result = Map.of(
                        "message", "There is no incident to investigate yet. "
                                + "Ask me to check service health, then run "
                                + "incident detection if a target is unavailable.");
                steps.add(step("list_incidents", all));
            } else {
                JsonNode analysis = ai.investigate(selected);

                result = Map.of(
                        "incidentId", selected.getId(),
                        "serviceName", selected.getServiceName(),
                        "incident", selected,
                        "analysis", analysis,
                        "requiresHumanApproval", true);

                steps.add(step("list_incidents", all));
                steps.add(step("investigate_incident", result));
            }

        } else if (detectRequest) {
            result = detection.detectIncidents();
            steps.add(step("detect_incidents", result));

        } else if (healthRequest) {
            result = prometheus.query("up");
            steps.add(step("query_prometheus", result));

        } else {
            result = Map.of(
                    "message", "I can check live service health, list recorded "
                            + "incidents, detect new incidents, or investigate "
                            + "an existing incident.",
                    "availableActions", List.of(
                            "Check service health",
                            "Detect incidents",
                            "List incidents",
                            "Investigate the latest incident"));

            steps.add(step("help", result));
        }

        return Map.of(
                "agent", "OpsPilot",
                "userMessage", message,
                "steps", steps,
                "result", result,
                "note", "Results come from OpsPilot tools. "
                        + "Recovery actions require explicit human approval.");
    }

    private Map<String, Object> step(String tool, Object output) {
        return Map.of(
                "tool", tool,
                "status", "completed",
                "output", output);
    }
}
