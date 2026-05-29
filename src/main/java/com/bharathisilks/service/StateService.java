package com.bharathisilks.service;

import com.bharathisilks.repo.CounterRepository;
import com.bharathisilks.repo.CustomerRepository;
import com.bharathisilks.repo.OrderRepository;
import com.bharathisilks.repo.ProductRepository;
import com.bharathisilks.repo.PurchaseRepository;
import com.bharathisilks.repo.SaleRepository;
import com.bharathisilks.web.dto.StateResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StateService {

    private final ProductRepository products;
    private final SaleRepository sales;
    private final PurchaseRepository purchases;
    private final CustomerRepository customers;
    private final CounterRepository counters;
    private final CategoryService categories;
    private final OrderRepository orders;

    public StateService(ProductRepository products, SaleRepository sales, PurchaseRepository purchases,
                        CustomerRepository customers, CounterRepository counters, CategoryService categories,
                        OrderRepository orders) {
        this.products = products;
        this.sales = sales;
        this.purchases = purchases;
        this.customers = customers;
        this.counters = counters;
        this.categories = categories;
        this.orders = orders;
    }

    /** One round-trip snapshot the UI loads on startup and after each mutation. */
    @Transactional(readOnly = true)
    public StateResponse snapshot() {
        Map<String, Integer> counterMap = new LinkedHashMap<>();
        counters.findAll().forEach(c -> counterMap.put(c.getPrefix(), c.getValue()));
        return new StateResponse(
                products.findAllByOrderByCreatedDesc(),
                sales.findAllByOrderByDateAsc(),
                purchases.findAllByOrderByDateAsc(),
                customers.findAll(),
                counterMap,
                categories.names(),
                orders.findAllByOrderByDateDesc());
    }
}
