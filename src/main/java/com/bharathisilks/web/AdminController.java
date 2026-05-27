package com.bharathisilks.web;

import com.bharathisilks.service.SeedService;
import com.bharathisilks.service.StateService;
import com.bharathisilks.web.dto.StateResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SeedService seedService;
    private final StateService stateService;

    public AdminController(SeedService seedService, StateService stateService) {
        this.seedService = seedService;
        this.stateService = stateService;
    }

    /** Wipes all data and reloads the demo catalogue. */
    @PostMapping("/reset")
    public StateResponse reset() {
        seedService.reset();
        return stateService.snapshot();
    }
}
