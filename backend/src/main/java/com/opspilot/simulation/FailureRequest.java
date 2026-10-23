package com.opspilot.simulation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record FailureRequest(
    @NotBlank String serviceName,
    @NotBlank @Pattern(regexp = "HIGH_LATENCY|HIGH_ERROR_RATE|DB_POOL_EXHAUSTION|KAFKA_CONSUMER_LAG|DEPLOYMENT_REGRESSION|SERVICE_DOWN")
    String failureType
) {}
