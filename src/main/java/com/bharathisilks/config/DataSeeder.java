package com.bharathisilks.config;

import com.bharathisilks.service.SeedService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Seeds the demo catalogue on startup when the database is empty. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final SeedService seedService;

    public DataSeeder(SeedService seedService) {
        this.seedService = seedService;
    }

    @Override
    public void run(String... args) {
        if (seedService.isEmpty()) {
            seedService.seed();
        }
    }
}
