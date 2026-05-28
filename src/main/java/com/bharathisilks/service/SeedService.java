package com.bharathisilks.service;

import com.bharathisilks.domain.Category;
import com.bharathisilks.domain.Product;
import com.bharathisilks.repo.CounterRepository;
import com.bharathisilks.repo.CustomerRepository;
import com.bharathisilks.repo.ProductRepository;
import com.bharathisilks.repo.PurchaseRepository;
import com.bharathisilks.repo.SaleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeedService {

    private record Seed(String name, String category, double price, double cost,
                        int stock, String size, String color) {
    }

    private static final List<Seed> SEED = List.of(
            new Seed("Kanchipuram Silk Saree", "Sarees", 8500, 6200, 6, "—", "Magenta"),
            new Seed("Banarasi Silk Saree", "Sarees", 6200, 4500, 2, "—", "Royal Blue"),
            new Seed("Mysore Silk Saree", "Sarees", 4800, 3500, 0, "—", "Green"),
            new Seed("Bridal Lehenga", "Lehengas", 18500, 13000, 3, "M", "Maroon"),
            new Seed("Designer Anarkali Dress", "Dresses", 3200, 2100, 10, "L", "Teal"),
            new Seed("Embroidered Kurti", "Kurtis", 1450, 900, 15, "M", "Mustard"));

    private final ProductRepository products;
    private final SaleRepository sales;
    private final PurchaseRepository purchases;
    private final CustomerRepository customers;
    private final CounterRepository counters;
    private final SkuService skuService;
    private final CategoryService categories;
    private final AuditService audit;

    public SeedService(ProductRepository products, SaleRepository sales, PurchaseRepository purchases,
                       CustomerRepository customers, CounterRepository counters, SkuService skuService,
                       CategoryService categories, AuditService audit) {
        this.products = products;
        this.sales = sales;
        this.purchases = purchases;
        this.customers = customers;
        this.counters = counters;
        this.skuService = skuService;
        this.categories = categories;
        this.audit = audit;
    }

    public boolean isEmpty() {
        return products.count() == 0;
    }

    @Transactional
    public void reset() {
        sales.deleteAll();
        purchases.deleteAll();
        customers.deleteAll();
        products.deleteAll();
        counters.deleteAll();
        categories.resetToDefaults();
        audit.clear();
        seed();
        audit.record("data.reset", "data", "", "demo data reseeded");
    }

    @Transactional
    public void seed() {
        categories.seedDefaults();
        long base = System.currentTimeMillis();
        for (int i = 0; i < SEED.size(); i++) {
            Seed s = SEED.get(i);
            Product p = new Product();
            p.setName(s.name());
            p.setCategory(s.category());
            p.setSize(s.size());
            p.setColor(s.color());
            p.setCost(s.cost());
            p.setPrice(s.price());
            p.setStock(s.stock());
            p.setGst(RetailRules.gstRate(s.price()));
            Category cat = categories.ensure(s.category());
            p.setCategory(cat.getName());
            p.setSku(skuService.next(cat.getPrefix()));
            // Keep the seeded display order (first item shown on top) under "newest first".
            p.setCreated(base + (SEED.size() - i));
            products.save(p);
        }
    }
}
