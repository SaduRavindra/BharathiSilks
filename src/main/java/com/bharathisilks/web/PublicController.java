package com.bharathisilks.web;

import com.bharathisilks.domain.Order;
import com.bharathisilks.service.OrderService;
import com.bharathisilks.service.ProductService;
import com.bharathisilks.web.dto.PlaceOrderRequest;
import com.bharathisilks.web.dto.PublicProductView;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Unauthenticated storefront data + order placement/tracking. */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final ProductService products;
    private final OrderService orders;

    public PublicController(ProductService products, OrderService orders) {
        this.products = products;
        this.orders = orders;
    }

    @GetMapping("/products")
    public List<PublicProductView> products() {
        return products.list().stream()
                .map(p -> new PublicProductView(
                        p.getSku(), p.getName(), p.getCategory(),
                        p.getStyleCode(), p.getFabric(), p.getDesign(),
                        p.getSize(), p.getColor(), p.getImageUrl(), p.getPrice(), p.getStock() > 0))
                .toList();
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Order placeOrder(@RequestBody PlaceOrderRequest req) {
        return orders.place(req);
    }

    @GetMapping("/orders/{ref}")
    public Order track(@PathVariable String ref) {
        return orders.get(ref);
    }
}
