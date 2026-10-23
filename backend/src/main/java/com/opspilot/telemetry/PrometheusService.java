package com.opspilot.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PrometheusService {
    private final RestClient restClient;
    private final String prometheusUrl;

    public PrometheusService(
            @Value("${opspilot.prometheus.base-url:http://localhost:9091}")
            String prometheusUrl) {
        this.prometheusUrl = prometheusUrl;
        this.restClient = RestClient.create();
    }

    public JsonNode query(String expression) {
        var uri = UriComponentsBuilder.fromUriString(prometheusUrl)
                .path("/api/v1/query")
                .queryParam("query", expression)
                .build()
                .encode()
                .toUri();

        JsonNode response = restClient.get().uri(uri)
                .retrieve().body(JsonNode.class);

        if (response == null || !"success".equals(response.path("status").asText())) {
            throw new IllegalStateException("Prometheus query did not succeed");
        }
        return response;
    }

    public JsonNode queryUnavailableTargets() {
        return query("up == 0");
    }

    public JsonNode queryHttp5xxErrorRate() {
        return query("""
            100 * sum by (job) (rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
            / clamp_min(sum by (job) (rate(http_server_requests_seconds_count[5m])), 0.001)
            """);
    }

    public JsonNode queryHttp4xxErrorRate() {
        return query("""
            100 * sum by (job) (rate(http_server_requests_seconds_count{status=~"4.."}[5m]))
            / clamp_min(sum by (job) (rate(http_server_requests_seconds_count[5m])), 0.001)
            """);
    }

    public JsonNode queryHttpStatusCounts() {
        return query("sum by (job, status) (http_server_requests_seconds_count)");
    }

    public JsonNode queryAverageLatencyByJob() {
        return query("""
            sum by (job) (rate(http_server_requests_seconds_sum[5m]))
            / clamp_min(sum by (job) (rate(http_server_requests_seconds_count[5m])), 0.001)
            """);
    }

    public JsonNode queryMaxLatencyByJob() {
        return query("max by (job) (http_server_requests_seconds_max)");
    }

    public JsonNode queryRequestRateByJob() {
        return query("sum by (job) (rate(http_server_requests_seconds_count[5m]))");
    }
}