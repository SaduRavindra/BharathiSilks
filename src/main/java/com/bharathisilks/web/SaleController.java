package com.bharathisilks.web;

import com.bharathisilks.domain.Sale;
import com.bharathisilks.service.SaleService;
import com.bharathisilks.web.dto.SaleRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService service;

    public SaleController(SaleService service) {
        this.service = service;
    }

    @GetMapping
    public List<Sale> all() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Sale complete(@RequestBody SaleRequest req) {
        return service.complete(req);
    }

    @PostMapping("/{inv}/return")
    public Sale returnSale(@PathVariable String inv) {
        return service.returnSale(inv);
    }
}
