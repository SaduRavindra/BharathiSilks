package com.bharathisilks.service;

import com.bharathisilks.domain.Product;
import com.bharathisilks.error.NotFoundException;
import com.bharathisilks.repo.ProductRepository;
import com.bharathisilks.web.dto.ProductRequest;
import com.bharathisilks.web.dto.ProductUpdateRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository products;
    private final SkuService skuService;

    public ProductService(ProductRepository products, SkuService skuService) {
        this.products = products;
        this.skuService = skuService;
    }

    public List<Product> list() {
        return products.findAllByOrderByCreatedDesc();
    }

    public Product get(String sku) {
        return products.findBySku(sku)
                .orElseThrow(() -> new NotFoundException("No product with SKU " + sku));
    }

    @Transactional
    public Product create(ProductRequest req) {
        String name = req.name() == null ? "" : req.name().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        String category = RetailRules.CATEGORIES.contains(req.category()) ? req.category() : "Other";
        double price = req.price() == null ? 0 : req.price();
        if (price <= 0) {
            throw new IllegalArgumentException("A valid selling price is required");
        }

        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setSize(req.size() == null || req.size().isBlank() ? "—" : req.size());
        p.setColor(req.color() == null ? "" : req.color().trim());
        p.setCost(Math.max(0, req.cost() == null ? 0 : req.cost()));
        p.setPrice(price);
        p.setStock(Math.max(0, req.stock() == null ? 0 : req.stock()));
        p.setGst(RetailRules.gstRate(price));
        p.setSku(skuService.next(category));
        p.setCreated(System.currentTimeMillis());
        return products.save(p);
    }

    @Transactional
    public Product update(String sku, ProductUpdateRequest req) {
        Product p = get(sku);
        if (req.price() != null) {
            double price = Math.max(0, req.price());
            p.setPrice(price);
            p.setGst(RetailRules.gstRate(price));
        }
        if (req.stock() != null) {
            p.setStock(Math.max(0, req.stock()));
        }
        return products.save(p);
    }

    @Transactional
    public void delete(String sku) {
        Product p = get(sku);
        products.delete(p);
    }
}
