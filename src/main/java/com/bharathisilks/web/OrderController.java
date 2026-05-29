package com.bharathisilks.web;

import com.bharathisilks.domain.Order;
import com.bharathisilks.service.OrderService;
import com.bharathisilks.web.dto.OrderStatusRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Staff-facing order management (any signed-in user). */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    public List<Order> all() {
        return orders.list();
    }

    @PostMapping("/{ref}/status")
    public Order status(@PathVariable String ref, @RequestBody OrderStatusRequest req) {
        return orders.updateStatus(ref, req.status(), req.note());
    }

    @PostMapping("/{ref}/fulfil")
    public Order fulfil(@PathVariable String ref) {
        return orders.fulfil(ref);
    }
}
