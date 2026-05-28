package com.bharathisilks.web;

import com.bharathisilks.service.ReportService;
import com.bharathisilks.web.dto.ReportResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping
    public ReportResponse report(@RequestParam(defaultValue = "30") int days) {
        return service.report(days);
    }
}
