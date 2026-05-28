package com.bharathisilks.service;

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
                        int stock, String size, String color, String fabric, String design) {
    }

    private static final List<Seed> SEED = List.of(
            new Seed("Kanchipuram Silk Saree", "Sarees", 8500, 6200, 6, "—", "Magenta", "Kanjivaram Silk", "Temple border"),
            new Seed("Banarasi Silk Saree", "Sarees", 6200, 4500, 2, "—", "Royal Blue", "Banarasi Silk", "Zari butta"),
            new Seed("Mysore Silk Saree", "Sarees", 4800, 3500, 0, "—", "Green", "Mysore Silk", "Plain weave"),
            new Seed("Bridal Lehenga", "Lehengas", 18500, 13000, 3, "M", "Maroon", "Silk blend", "Heavy embroidery"),
            new Seed("Designer Anarkali Dress", "Dresses", 3200, 2100, 10, "L", "Teal", "Georgette", "Anarkali flare"),
            new Seed("Embroidered Kurti", "Kurtis", 1450, 900, 15, "M", "Mustard", "Cotton silk", "Thread work"));

    private final ProductRepository products;
    private final SaleRepository sales;
    private final PurchaseRepository purchases;
    private final CustomerRepository customers;
    private final CounterRepository counters;
    private final SkuService skuService;

    public SeedService(ProductRepository products, SaleRepository sales, PurchaseRepository purchases,
                       CustomerRepository customers, CounterRepository counters, SkuService skuService) {
        this.products = products;
        this.sales = sales;
        this.purchases = purchases;
        this.customers = customers;
        this.counters = counters;
        this.skuService = skuService;
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
        seed();
    }

    @Transactional
    public void seed() {
        long base = System.currentTimeMillis();
        for (int i = 0; i < SEED.size(); i++) {
            Seed s = SEED.get(i);
            Product p = new Product();
            p.setName(s.name());
            p.setCategory(s.category());
            p.setStyleCode("SEED-" + (i + 1));
            p.setFabric(s.fabric());
            p.setDesign(s.design());
            p.setSize(s.size());
            p.setColor(s.color());
            p.setCost(s.cost());
            p.setPrice(s.price());
            p.setStock(s.stock());
            p.setGst(RetailRules.gstRate(s.price()));
            p.setSku(skuService.next(s.category()));
            // Keep the seeded display order (first item shown on top) under "newest first".
            p.setCreated(base + (SEED.size() - i));
            products.save(p);
        }
    }
}
