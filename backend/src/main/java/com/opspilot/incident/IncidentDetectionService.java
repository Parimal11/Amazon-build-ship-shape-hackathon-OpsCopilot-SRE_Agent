
package com.opspilot.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.opspilot.telemetry.PrometheusService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class IncidentDetectionService {

    private static final double ERROR_RATE_THRESHOLD_PERCENT = 5.0;

    private static final List<IncidentStatus> OPEN_STATUSES =
            List.of(
                    IncidentStatus.DETECTED,
                    IncidentStatus.INVESTIGATING,
                    IncidentStatus.MITIGATING
            );

    private final PrometheusService prometheusService;
    private final IncidentRepository incidentRepository;

    public IncidentDetectionService(
            PrometheusService prometheusService,
            IncidentRepository incidentRepository) {
        this.prometheusService = prometheusService;
        this.incidentRepository = incidentRepository;
    }

    @Transactional
    public List<Incident> detectIncidents() {
        List<Incident> detected = new ArrayList<>();

        detectUnavailableTargets(detected);
        detectHighErrorRates(detected);
        detectHigh4xxRates(detected);
        detectHighAverageLatency(detected);

        return detected;
    }

    private void detectUnavailableTargets(List<Incident> detected) {
        JsonNode response = prometheusService.queryUnavailableTargets();
        JsonNode results = response.path("data").path("result");

        for (JsonNode result : results) {
            String serviceName =
                    result.path("metric").path("job").asText("unknown");

            String summary = "Prometheus detected unavailable target";

            String evidence = "Prometheus metric up=0; job="
                    + serviceName
                    + "; instance="
                    + result.path("metric").path("instance").asText("unknown");

            createIfNew(
                    detected,
                    serviceName,
                    summary,
                    evidence,
                    IncidentSeverity.CRITICAL
            );
        }
    }

    private void detectHighErrorRates(List<Incident> detected) {
        JsonNode response = prometheusService.queryHttp5xxErrorRate();
        JsonNode results = response.path("data").path("result");

        for (JsonNode result : results) {
            String serviceName =
                    result.path("metric").path("job").asText("unknown");

            JsonNode value = result.path("value").path(1);
            if (!value.isNumber() && !value.isTextual()) {
                continue;
            }

            double errorRate;
            try {
                errorRate = Double.parseDouble(value.asText());
            } catch (NumberFormatException ex) {
                continue;
            }

            if (errorRate <= ERROR_RATE_THRESHOLD_PERCENT) {
                continue;
            }

            String summary = "HTTP 5xx error rate exceeded 5%";

            String evidence = String.format(
                    "Prometheus measured HTTP 5xx error rate %.2f%% "
                            + "over a 5-minute window; threshold=%.2f%%",
                    errorRate,
                    ERROR_RATE_THRESHOLD_PERCENT
            );

            createIfNew(
                    detected,
                    serviceName,
                    summary,
                    evidence,
                    IncidentSeverity.HIGH
            );
        }
    }


    private void detectHigh4xxRates(List<Incident> detected) {
        JsonNode response = prometheusService.queryHttp4xxErrorRate();
        JsonNode results = response.path("data").path("result");
        JsonNode trafficResponse = prometheusService.queryRequestRateByJob();
        java.util.Map<String, Double> requestRates = new java.util.HashMap<>();
        for (JsonNode traffic : trafficResponse.path("data").path("result")) {
            String job = traffic.path("metric").path("job").asText("unknown");
            try { requestRates.put(job, Double.parseDouble(traffic.path("value").path(1).asText())); }
            catch (NumberFormatException ignored) { }
        }

        for (JsonNode result : results) {
            String serviceName = result.path("metric").path("job").asText("unknown");
            double rate;
            try { rate = Double.parseDouble(result.path("value").path(1).asText()); }
            catch (Exception ex) { continue; }

            // Avoid alerting on tiny traffic volumes where a single 404 can distort the ratio.
            if (rate <= 10.0 || requestRates.getOrDefault(serviceName, 0.0) < 0.1) continue;

            createIfNew(detected, serviceName,
                    "HTTP 4xx error rate exceeded 10%",
                    String.format("Prometheus measured HTTP 4xx rate %.2f%% over 5 minutes; threshold=10%%; requestRate=%.3f requests/sec",
                            rate, requestRates.getOrDefault(serviceName, 0.0)),
                    IncidentSeverity.MEDIUM);
        }
    }

    private void detectHighAverageLatency(List<Incident> detected) {
        final double thresholdSeconds = 0.5;
        JsonNode response = prometheusService.queryAverageLatencyByJob();
        for (JsonNode result : response.path("data").path("result")) {
            String serviceName = result.path("metric").path("job").asText("unknown");
            double averageSeconds;
            try { averageSeconds = Double.parseDouble(result.path("value").path(1).asText()); }
            catch (Exception ex) { continue; }
            if (!Double.isFinite(averageSeconds) || averageSeconds <= thresholdSeconds) continue;

            createIfNew(detected, serviceName,
                    "Average HTTP request latency exceeded 500 ms",
                    String.format("Prometheus average HTTP request duration %.3f seconds over 5 minutes; threshold=0.500 seconds",
                            averageSeconds),
                    IncidentSeverity.HIGH);
        }
    }
    private void createIfNew(
            List<Incident> detected,
            String serviceName,
            String summary,
            String evidence,
            IncidentSeverity severity) {

        boolean alreadyOpen =
                incidentRepository.existsByServiceNameAndSummaryAndStatusIn(
                        serviceName,
                        summary,
                        OPEN_STATUSES
                );

        if (alreadyOpen) {
            return;
        }

        Incident incident = incidentRepository.save(
                new Incident(
                        serviceName,
                        severity,
                        IncidentStatus.DETECTED,
                        summary,
                        evidence
                )
        );

        detected.add(incident);
    }
}
