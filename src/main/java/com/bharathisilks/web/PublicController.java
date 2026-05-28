package com.bharathisilks.web;

import com.bharathisilks.service.ProductService;
import com.bharathisilks.web.dto.PublicProductView;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Unauthenticated, read-only data for the public storefront. */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final ProductService products;

    public PublicController(ProductService products) {
        this.products = products;
    }

    @GetMapping("/products")
    public List<PublicProductView> products() {
        return products.list().stream()
                .map(p -> new PublicProductView(
                        p.getSku(), p.getName(), p.getCategory(), p.getStyleCode(),
                        p.getFabric(), p.getDesign(), p.getSize(), p.getColor(),
                        p.getImageUrl(), p.getPrice(), p.getStock() > 0))
                .toList();
    }
}
