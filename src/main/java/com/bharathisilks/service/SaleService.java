package com.bharathisilks.service;

import com.bharathisilks.domain.Customer;
import com.bharathisilks.domain.Product;
import com.bharathisilks.domain.Sale;
import com.bharathisilks.domain.SaleItem;
import com.bharathisilks.error.NotFoundException;
import com.bharathisilks.repo.CustomerRepository;
import com.bharathisilks.repo.ProductRepository;
import com.bharathisilks.repo.SaleRepository;
import com.bharathisilks.web.dto.SaleRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService {

    private final ProductRepository products;
    private final SaleRepository sales;
    private final CustomerRepository customers;
    private final AuditService audit;

    public SaleService(ProductRepository products, SaleRepository sales, CustomerRepository customers,
                       AuditService audit) {
        this.products = products;
        this.sales = sales;
        this.customers = customers;
        this.audit = audit;
    }

    public List<Sale> list() {
        return sales.findAllByOrderByDateAsc();
    }

    /**
     * Prices the cart against live product records, validates stock, applies the
     * discount/loyalty rules, decrements inventory, and updates the customer.
     */
    @Transactional
    public Sale complete(SaleRequest req) {
        Map<String, Integer> qtyBySku = mergeLines(req.items());
        if (qtyBySku.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        List<SaleItem> lines = new ArrayList<>();
        double sub = 0;
        double tax = 0;
        double profit = 0;
        for (Map.Entry<String, Integer> entry : qtyBySku.entrySet()) {
            Product p = products.findBySku(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown SKU " + entry.getKey()));
            int qty = entry.getValue();
            if (qty > p.getStock()) {
                throw new IllegalArgumentException(p.getName() + ": only " + p.getStock() + " in stock");
            }
            int gst = p.getGst() > 0 ? p.getGst() : RetailRules.gstRate(p.getPrice());

            SaleItem item = new SaleItem();
            item.setSku(p.getSku());
            item.setName(p.getName());
            item.setPrice(p.getPrice());
            item.setCost(p.getCost());
            item.setGst(gst);
            item.setQty(qty);
            lines.add(item);

            sub += p.getPrice() * qty;
            tax += Math.round(p.getPrice() * qty * gst / 100.0);
            profit += (p.getPrice() - p.getCost()) * qty;
        }

        double disc = discount(sub, req);

        String phone = req.phone() == null ? "" : req.phone().trim();
        Customer customer = phone.isEmpty() ? null : customers.findById(phone).orElse(null);

        double redeem = 0;
        if (Boolean.TRUE.equals(req.redeem()) && customer != null) {
            redeem = Math.max(0, Math.min(customer.getPoints(), sub - disc));
        }

        double total = Math.max(0, sub - disc + tax - redeem);
        profit -= disc;

        for (SaleItem item : lines) {
            Product p = products.findBySku(item.getSku()).orElseThrow();
            p.setStock(Math.max(0, p.getStock() - item.getQty()));
        }

        String customerName = "";
        if (!phone.isEmpty()) {
            customer = applyLoyalty(customer, phone, req.name(), redeem, total);
            customerName = customer.getName();
        }

        Sale sale = new Sale();
        sale.setInv("INV-%05d".formatted(sales.count() + 1));
        sale.setDate(System.currentTimeMillis());
        sale.setSub(sub);
        sale.setTax(tax);
        sale.setDisc(disc);
        sale.setRedeem(redeem);
        sale.setTotal(total);
        sale.setPay(req.pay() == null || req.pay().isBlank() ? "Cash" : req.pay());
        sale.setPhone(phone);
        sale.setName(customerName);
        sale.setProfit(profit);
        sale.setReturned(false);
        lines.forEach(sale::addItem);
        Sale saved = sales.save(sale);
        audit.record("sale.complete", "sale", saved.getInv(), "total=" + saved.getTotal());
        return saved;
    }

    /** Restocks the items and reverses loyalty for a previously completed bill. */
    @Transactional
    public Sale returnSale(String inv) {
        Sale sale = sales.findByInv(inv)
                .orElseThrow(() -> new NotFoundException("No invoice " + inv));
        if (sale.isReturned()) {
            return sale;
        }
        for (SaleItem item : sale.getItems()) {
            products.findBySku(item.getSku())
                    .ifPresent(p -> p.setStock(p.getStock() + item.getQty()));
        }
        if (sale.getPhone() != null && !sale.getPhone().isEmpty()) {
            customers.findById(sale.getPhone()).ifPresent(c -> {
                c.setSpend(Math.max(0, c.getSpend() - sale.getTotal()));
                int reverse = (int) Math.floor(sale.getTotal() / RetailRules.POINTS_PER);
                c.setPoints(Math.max(0, c.getPoints() - reverse));
                c.setVisits(Math.max(0, c.getVisits() - 1));
            });
        }
        sale.setReturned(true);
        Sale saved = sales.save(sale);
        audit.record("sale.return", "sale", inv, "restocked & loyalty reversed");
        return saved;
    }

    private Map<String, Integer> mergeLines(List<SaleRequest.Line> requested) {
        Map<String, Integer> qtyBySku = new LinkedHashMap<>();
        if (requested == null) {
            return qtyBySku;
        }
        for (SaleRequest.Line line : requested) {
            if (line == null || line.sku() == null) {
                continue;
            }
            int qty = line.qty() == null ? 0 : line.qty();
            if (qty > 0) {
                qtyBySku.merge(line.sku().trim(), qty, Integer::sum);
            }
        }
        return qtyBySku;
    }

    private double discount(double sub, SaleRequest req) {
        double requested = req.disc() == null ? 0 : req.disc();
        if (requested <= 0) {
            return 0;
        }
        boolean percent = req.discType() == null || "%".equals(req.discType());
        return percent
                ? Math.round(sub * Math.min(requested, 100) / 100.0)
                : Math.min(requested, sub);
    }

    private Customer applyLoyalty(Customer existing, String phone, String name, double redeem, double total) {
        Customer customer = existing;
        if (customer == null) {
            customer = new Customer();
            customer.setPhone(phone);
            customer.setName(name == null || name.isBlank() ? "Guest" : name.trim());
        }
        if (name != null && !name.isBlank()) {
            customer.setName(name.trim());
        }
        if (redeem > 0) {
            customer.setPoints((int) Math.max(0, customer.getPoints() - Math.round(redeem)));
        }
        customer.setPoints(customer.getPoints() + (int) Math.floor(total / RetailRules.POINTS_PER));
        customer.setSpend(customer.getSpend() + total);
        customer.setVisits(customer.getVisits() + 1);
        customer.setLastSeen(System.currentTimeMillis());
        return customers.save(customer);
    }
}
