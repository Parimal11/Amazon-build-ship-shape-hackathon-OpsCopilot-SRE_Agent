package com.opspilot.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {
    private final PrometheusService prometheusService;

    private static final Set<String> ALLOWED_QUERIES = Set.of(
            "up",
            "process_cpu_usage",
            "http_server_requests_seconds_count",
            "up == 0",
            "HTTP_5XX_ERROR_RATE",
            "HTTP_4XX_ERROR_RATE",
            "HTTP_STATUS_COUNTS",
            "HTTP_AVERAGE_LATENCY",
            "HTTP_MAX_LATENCY",
            "HTTP_REQUEST_RATE"
    );

    public TelemetryController(PrometheusService prometheusService) {
        this.prometheusService = prometheusService;
    }

    @GetMapping("/query")
    public JsonNode query(@RequestParam(defaultValue = "up") String expression) {
        if (!ALLOWED_QUERIES.contains(expression)) {
            throw new IllegalArgumentException("Unsupported query: " + expression);
        }

        return switch (expression) {
            case "up == 0" -> prometheusService.queryUnavailableTargets();
            case "HTTP_5XX_ERROR_RATE" -> prometheusService.queryHttp5xxErrorRate();
            case "HTTP_4XX_ERROR_RATE" -> prometheusService.queryHttp4xxErrorRate();
            case "HTTP_STATUS_COUNTS" -> prometheusService.queryHttpStatusCounts();
            case "HTTP_AVERAGE_LATENCY" -> prometheusService.queryAverageLatencyByJob();
            case "HTTP_MAX_LATENCY" -> prometheusService.queryMaxLatencyByJob();
            case "HTTP_REQUEST_RATE" -> prometheusService.queryRequestRateByJob();
            default -> prometheusService.query(expression);
        };
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        JsonNode response = prometheusService.query("up");
        return Map.of(
                "connected", true,
                "prometheusStatus", response.path("status").asText(),
                "data", response.path("data")
        );
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("availability", prometheusService.query("up"));
        summary.put("httpStatusCounts", prometheusService.queryHttpStatusCounts());
        summary.put("http5xxErrorRatePercent", prometheusService.queryHttp5xxErrorRate());
        summary.put("http4xxErrorRatePercent", prometheusService.queryHttp4xxErrorRate());
        summary.put("averageLatencySeconds", prometheusService.queryAverageLatencyByJob());
        summary.put("maxRecordedLatencySeconds", prometheusService.queryMaxLatencyByJob());
        summary.put("requestRatePerSecond", prometheusService.queryRequestRateByJob());
        summary.put("latencyNote", "Average latency is derived from _sum/_count. p95/p99 is unavailable until application histogram buckets are exposed.");
        return summary;
    }
}