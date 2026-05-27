package com.bharathisilks.service;

import com.bharathisilks.domain.Customer;
import com.bharathisilks.error.NotFoundException;
import com.bharathisilks.repo.CustomerRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository customers;

    public CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    public List<Customer> list() {
        return customers.findAll().stream()
                .sorted(Comparator.comparingDouble(Customer::getSpend).reversed())
                .toList();
    }

    public Customer get(String phone) {
        return customers.findById(phone)
                .orElseThrow(() -> new NotFoundException("No customer with phone " + phone));
    }
}
