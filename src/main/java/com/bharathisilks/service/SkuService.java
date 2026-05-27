package com.bharathisilks.service;

import com.bharathisilks.domain.Counter;
import com.bharathisilks.repo.CounterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkuService {

    private final CounterRepository counters;

    public SkuService(CounterRepository counters) {
        this.counters = counters;
    }

    /** Mints the next SKU for a category, e.g. {@code BS-SAR-0007}. */
    @Transactional
    public String next(String category) {
        String prefix = RetailRules.prefixFor(category);
        Counter counter = counters.findById(prefix).orElseGet(() -> {
            Counter created = new Counter();
            created.setPrefix(prefix);
            created.setValue(0);
            return created;
        });
        counter.setValue(counter.getValue() + 1);
        counters.save(counter);
        return "BS-%s-%04d".formatted(prefix, counter.getValue());
    }
}
