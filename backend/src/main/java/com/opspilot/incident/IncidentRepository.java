package com.opspilot.incident;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findAllByOrderByCreatedAtDesc();

    boolean existsByServiceNameAndSummaryAndStatusIn(
        String serviceName,
        String summary,
        List<IncidentStatus> statuses
    );
}
