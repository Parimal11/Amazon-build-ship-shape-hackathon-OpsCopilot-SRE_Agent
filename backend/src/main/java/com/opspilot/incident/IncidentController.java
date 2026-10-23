package com.opspilot.incident;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {
    private final IncidentRepository repository;
    public IncidentController(IncidentRepository repository) { this.repository = repository; }

    @GetMapping
    public List<Incident> list() { return repository.findAllByOrderByCreatedAtDesc(); }

    @GetMapping("/{id}")
    public ResponseEntity<Incident> get(@PathVariable Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
