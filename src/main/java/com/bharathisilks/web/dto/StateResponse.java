package com.bharathisilks.web.dto;

import com.bharathisilks.domain.Customer;
import com.bharathisilks.domain.Product;
import com.bharathisilks.domain.Purchase;
import com.bharathisilks.domain.Sale;
import java.util.List;
import java.util.Map;

/** Full application snapshot, matching the shape the storefront UI expects. */
public record StateResponse(
        List<Product> products,
        List<Sale> sales,
        List<Purchase> purchases,
        List<Customer> customers,
        Map<String, Integer> counters) {
}
