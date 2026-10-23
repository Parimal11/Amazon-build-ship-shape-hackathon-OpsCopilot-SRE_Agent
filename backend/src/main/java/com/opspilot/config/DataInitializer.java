package com.opspilot.config;

import com.opspilot.registry.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedServices(ServiceRepository repository) {
        return args -> {
            seed(repository, "payment-service", ServiceStatus.HEALTHY, 0.4, 120, 32, 35, 0);
            seed(repository, "ledger-service", ServiceStatus.HEALTHY, 0.2, 95, 28, 30, 0);
            seed(repository, "order-service", ServiceStatus.HEALTHY, 0.3, 110, 25, 25, 0);
            seed(repository, "notification-service", ServiceStatus.HEALTHY, 0.1, 80, 20, 15, 0);
        };
    }

    private void seed(ServiceRepository repository, String name, ServiceStatus status, double error, double p95,
                      double cpu, double db, long lag) {
        if (repository.findByName(name).isEmpty()) {
            repository.save(new ServiceEntity(name, "local", status, error, p95, cpu, db, lag));
        }
    }
}
