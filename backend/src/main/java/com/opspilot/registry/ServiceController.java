package com.opspilot.registry;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {
    private final ServiceRepository repository;
    public ServiceController(ServiceRepository repository) { this.repository = repository; }

    @GetMapping
    public List<ServiceEntity> list() { return repository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceEntity> get(@PathVariable Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/health")
    public ResponseEntity<ServiceEntity> health(@PathVariable Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
