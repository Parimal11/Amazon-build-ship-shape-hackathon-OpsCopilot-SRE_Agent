package com.opspilot.incident;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "incidents")
public class Incident {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String serviceName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentSeverity severity;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;
    @Column(nullable = false, length = 2000)
    private String summary;
    @Column(length = 4000)
    private String evidence;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant resolvedAt;

    protected Incident() {}

    public Incident(String serviceName, IncidentSeverity severity, IncidentStatus status, String summary, String evidence) {
        this.serviceName = serviceName;
        this.severity = severity;
        this.status = status;
        this.summary = summary;
        this.evidence = evidence;
        this.createdAt = Instant.now();
    }

    public void resolve() { this.status = IncidentStatus.RESOLVED; this.resolvedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getServiceName() { return serviceName; }
    public IncidentSeverity getSeverity() { return severity; }
    public IncidentStatus getStatus() { return status; }
    public String getSummary() { return summary; }
    public String getEvidence() { return evidence; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
}
