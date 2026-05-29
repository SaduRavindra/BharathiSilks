package com.bharathisilks.service;

import com.bharathisilks.domain.Order;
import com.bharathisilks.domain.OrderEvent;
import com.bharathisilks.domain.OrderItem;
import com.bharathisilks.domain.Product;
import com.bharathisilks.domain.Sale;
import com.bharathisilks.error.NotFoundException;
import com.bharathisilks.repo.OrderRepository;
import com.bharathisilks.repo.ProductRepository;
import com.bharathisilks.web.dto.PlaceOrderRequest;
import com.bharathisilks.web.dto.SaleRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    /** Forward fulfilment pipeline; CANCELLED is reachable from any state. */
    public static final List<String> FLOW = List.of("PLACED", "CONFIRMED", "PACKED", "SHIPPED", "DELIVERED");
    public static final String CANCELLED = "CANCELLED";

    private final OrderRepository orders;
    private final ProductRepository products;
    private final AuditService audit;
    private final SaleService sales;

    public OrderService(OrderRepository orders, ProductRepository products, AuditService audit,
                        SaleService sales) {
        this.orders = orders;
        this.products = products;
        this.audit = audit;
        this.sales = sales;
    }

    public List<Order> list() {
        return orders.findAllByOrderByDateDesc();
    }

    public Order get(String ref) {
        return orders.findByRef(ref)
                .orElseThrow(() -> new NotFoundException("No order " + ref));
    }

    @Transactional
    public Order place(PlaceOrderRequest req) {
        String name = req.name() == null ? "" : req.name().trim();
        String phone = req.phone() == null ? "" : req.phone().trim();
        if (name.isEmpty() || phone.isEmpty()) {
            throw new IllegalArgumentException("Name and mobile number are required");
        }
        Map<String, Integer> qtyBySku = new LinkedHashMap<>();
        if (req.items() != null) {
            for (PlaceOrderRequest.Line line : req.items()) {
                if (line == null || line.sku() == null) {
                    continue;
                }
                int qty = line.qty() == null ? 0 : line.qty();
                if (qty > 0) {
                    qtyBySku.merge(line.sku().trim(), qty, Integer::sum);
                }
            }
        }
        if (qtyBySku.isEmpty()) {
            throw new IllegalArgumentException("Your cart is empty");
        }

        long now = System.currentTimeMillis();
        Order order = new Order();
        order.setRef(nextRef());
        order.setDate(now);
        order.setName(name);
        order.setPhone(phone);
        order.setAddress(req.address() == null ? "" : req.address().trim());
        order.setFulfilment("Pickup".equalsIgnoreCase(req.fulfilment()) ? "Pickup" : "Delivery");
        order.setNote(req.note() == null ? "" : req.note().trim());
        order.setStatus("PLACED");
        order.getTimeline().add(new OrderEvent("PLACED", now));

        double total = 0;
        for (Map.Entry<String, Integer> e : qtyBySku.entrySet()) {
            Product p = products.findBySku(e.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown item " + e.getKey()));
            OrderItem item = new OrderItem();
            item.setSku(p.getSku());
            item.setName(p.getName());
            item.setPrice(p.getPrice());
            item.setQty(e.getValue());
            item.setImageUrl(p.getImageUrl());
            order.addItem(item);
            total += p.getPrice() * e.getValue();
        }
        order.setTotal(total);
        Order saved = orders.save(order);
        audit.record("order.place", "order", saved.getRef(), name + " · " + saved.getItems().size() + " item(s)");
        return saved;
    }

    @Transactional
    public Order updateStatus(String ref, String status, String note) {
        Order order = get(ref);
        String s = status == null ? "" : status.trim().toUpperCase();
        if (!FLOW.contains(s) && !CANCELLED.equals(s)) {
            throw new IllegalArgumentException("Unknown status " + status);
        }
        order.setStatus(s);
        order.getTimeline().add(new OrderEvent(s, System.currentTimeMillis()));
        if (note != null && !note.isBlank()) {
            order.setNote(note.trim());
        }
        Order saved = orders.save(order);
        audit.record("order.status", "order", ref, s);
        return saved;
    }

    private String nextRef() {
        return "ORD-%05d".formatted(orders.count() + 1001);
    }

    /**
     * Converts an order into a real sale: prices against live products, decrements
     * stock and awards loyalty (via SaleService), marks the order DELIVERED and
     * records the invoice. The saleInv guard prevents double-billing.
     */
    @Transactional
    public Order fulfil(String ref) {
        Order order = get(ref);
        if (order.getSaleInv() != null && !order.getSaleInv().isBlank()) {
            throw new IllegalArgumentException("Order " + ref + " is already billed (" + order.getSaleInv() + ")");
        }
        if (CANCELLED.equals(order.getStatus() == null ? "" : order.getStatus().toUpperCase())) {
            throw new IllegalArgumentException("A cancelled order can't be fulfilled");
        }
        List<SaleRequest.Line> lines = new ArrayList<>();
        for (OrderItem it : order.getItems()) {
            lines.add(new SaleRequest.Line(it.getSku(), it.getQty()));
        }
        Sale sale = sales.complete(new SaleRequest(lines, order.getPhone(), order.getName(), 0.0, "%", "Cash", false));
        order.setSaleInv(sale.getInv());
        long now = System.currentTimeMillis();
        order.setStatus("DELIVERED");
        order.getTimeline().add(new OrderEvent("DELIVERED", now));
        Order saved = orders.save(order);
        audit.record("order.fulfil", "order", ref, "billed " + sale.getInv());
        return saved;
    }
}
