package com.bharathisilks.web;

import com.bharathisilks.domain.Customer;
import com.bharathisilks.service.CustomerService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    public List<Customer> all() {
        return service.list();
    }

    @GetMapping("/{phone}")
    public Customer get(@PathVariable String phone) {
        return service.get(phone);
    }
}
