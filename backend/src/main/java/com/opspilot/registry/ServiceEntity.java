package com.opspilot.registry;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "monitored_services")
public class ServiceEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    @Column(nullable = false)
    private String environment = "local";
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceStatus status = ServiceStatus.HEALTHY;
    private double errorRatePercent;
    private double p95LatencyMs;
    private double cpuPercent;
    private double databaseConnectionsPercent;
    private long kafkaConsumerLag;
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected ServiceEntity() {}

    public ServiceEntity(String name, String environment, ServiceStatus status,
                         double errorRatePercent, double p95LatencyMs,
                         double cpuPercent, double databaseConnectionsPercent, long kafkaConsumerLag) {
        this.name = name;
        this.environment = environment;
        this.status = status;
        this.errorRatePercent = errorRatePercent;
        this.p95LatencyMs = p95LatencyMs;
        this.cpuPercent = cpuPercent;
        this.databaseConnectionsPercent = databaseConnectionsPercent;
        this.kafkaConsumerLag = kafkaConsumerLag;
        this.updatedAt = Instant.now();
    }

    public void applyFailure(String failureType) {
        switch (failureType) {
            case "HIGH_LATENCY" -> { this.status = ServiceStatus.DEGRADED; this.p95LatencyMs = 842; this.errorRatePercent = 2.4; }
            case "HIGH_ERROR_RATE", "DEPLOYMENT_REGRESSION" -> { this.status = ServiceStatus.DEGRADED; this.errorRatePercent = 8.4; this.p95LatencyMs = 640; }
            case "DB_POOL_EXHAUSTION" -> { this.status = ServiceStatus.DEGRADED; this.databaseConnectionsPercent = 98; this.errorRatePercent = 5.5; }
            case "KAFKA_CONSUMER_LAG" -> { this.status = ServiceStatus.DEGRADED; this.kafkaConsumerLag = 12430; }
            case "SERVICE_DOWN" -> { this.status = ServiceStatus.DOWN; this.errorRatePercent = 100; }
            default -> throw new IllegalArgumentException("Unsupported failure type: " + failureType);
        }
        this.updatedAt = Instant.now();
    }

    public void recover() {
        this.status = ServiceStatus.HEALTHY;
        this.errorRatePercent = 0.4;
        this.p95LatencyMs = 120;
        this.cpuPercent = 32;
        this.databaseConnectionsPercent = 35;
        this.kafkaConsumerLag = 0;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEnvironment() { return environment; }
    public ServiceStatus getStatus() { return status; }
    public double getErrorRatePercent() { return errorRatePercent; }
    public double getP95LatencyMs() { return p95LatencyMs; }
    public double getCpuPercent() { return cpuPercent; }
    public double getDatabaseConnectionsPercent() { return databaseConnectionsPercent; }
    public long getKafkaConsumerLag() { return kafkaConsumerLag; }
    public Instant getUpdatedAt() { return updatedAt; }
}
