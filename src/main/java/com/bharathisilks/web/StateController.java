package com.bharathisilks.web;

import com.bharathisilks.service.StateService;
import com.bharathisilks.web.dto.StateResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/state")
public class StateController {

    private final StateService stateService;

    public StateController(StateService stateService) {
        this.stateService = stateService;
    }

    @GetMapping
    public StateResponse state() {
        return stateService.snapshot();
    }
}
