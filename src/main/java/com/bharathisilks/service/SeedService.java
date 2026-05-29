package com.bharathisilks.service;

import com.bharathisilks.domain.Category;
import com.bharathisilks.domain.Order;
import com.bharathisilks.domain.OrderEvent;
import com.bharathisilks.domain.OrderItem;
import com.bharathisilks.domain.Product;
import com.bharathisilks.repo.CounterRepository;
import com.bharathisilks.repo.CustomerRepository;
import com.bharathisilks.repo.OrderRepository;
import com.bharathisilks.repo.ProductRepository;
import com.bharathisilks.repo.PurchaseRepository;
import com.bharathisilks.repo.SaleRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final CategoryService categories;
    private final AuditService audit;
    private final OrderRepository orders;

    public SeedService(ProductRepository products, SaleRepository sales, PurchaseRepository purchases,
                       CustomerRepository customers, CounterRepository counters, SkuService skuService,
                       CategoryService categories, AuditService audit, OrderRepository orders) {
        this.products = products;
        this.sales = sales;
        this.purchases = purchases;
        this.customers = customers;
        this.counters = counters;
        this.skuService = skuService;
        this.categories = categories;
        this.audit = audit;
        this.orders = orders;
    }

    public boolean isEmpty() {
        return products.count() == 0;
    }

    @Transactional
    public void reset() {
        orders.deleteAll();
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
            p.setStyleCode("SEED-" + (i + 1));
            p.setFabric(s.fabric());
            p.setDesign(s.design());
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
        seedOrders(base);
    }

    private void seedOrders(long base) {
        Map<String, Integer> shipped = new LinkedHashMap<>();
        shipped.put("BS-SAR-0001", 1);
        seedOrder("ORD-01001", base - 2L * 86400000L, "Anjali Menon", "9846012345",
                "12 Gandhi Road, Chennai 600001", "Delivery",
                List.of("PLACED", "CONFIRMED", "PACKED", "SHIPPED"), shipped);

        Map<String, Integer> placed = new LinkedHashMap<>();
        placed.put("BS-KUR-0001", 2);
        placed.put("BS-DRS-0001", 1);
        seedOrder("ORD-01002", base - 3L * 3600000L, "Karthik R", "9000022222",
                "", "Pickup", List.of("PLACED"), placed);
    }

    private void seedOrder(String ref, long date, String name, String phone, String address,
                           String fulfilment, List<String> statuses, Map<String, Integer> lines) {
        Order o = new Order();
        o.setRef(ref);
        o.setDate(date);
        o.setName(name);
        o.setPhone(phone);
        o.setAddress(address);
        o.setFulfilment(fulfilment);
        o.setNote("");
        double total = 0;
        for (Map.Entry<String, Integer> e : lines.entrySet()) {
            Product p = products.findBySku(e.getKey()).orElse(null);
            if (p == null) {
                continue;
            }
            OrderItem it = new OrderItem();
            it.setSku(p.getSku());
            it.setName(p.getName());
            it.setPrice(p.getPrice());
            it.setQty(e.getValue());
            it.setImageUrl(p.getImageUrl());
            o.addItem(it);
            total += p.getPrice() * e.getValue();
        }
        o.setTotal(total);
        for (int i = 0; i < statuses.size(); i++) {
            o.getTimeline().add(new OrderEvent(statuses.get(i), date + i * 3600000L));
        }
        o.setStatus(statuses.get(statuses.size() - 1));
        orders.save(o);
    }
}
