package com.bharathisilks.service;

import com.bharathisilks.domain.Product;
import com.bharathisilks.error.NotFoundException;
import com.bharathisilks.repo.ProductRepository;
import com.bharathisilks.web.dto.ProductRequest;
import com.bharathisilks.web.dto.ProductUpdateRequest;
import com.bharathisilks.web.dto.VariantMatrixRequest;
import java.util.ArrayList;
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
        String name = cleanRequired(req.name(), "Product name is required");
        String category = cleanCategory(req.category());
        double price = cleanPrice(req.price());
        Product p = buildProduct(
                name,
                category,
                cleanOptional(req.styleCode()),
                cleanOptional(req.fabric()),
                cleanOptional(req.design()),
                req.size() == null || req.size().isBlank() ? "—" : req.size().trim(),
                cleanOptional(req.color()),
                cleanImageUrl(req.imageUrl()),
                Math.max(0, req.cost() == null ? 0 : req.cost()),
                price,
                Math.max(0, req.stock() == null ? 0 : req.stock()),
                System.currentTimeMillis());
        return products.save(p);
    }

    @Transactional
    public List<Product> createMatrix(VariantMatrixRequest req) {
        String name = cleanRequired(req.name(), "Product name is required");
        String category = cleanCategory(req.category());
        double price = cleanPrice(req.price());
        String styleCode = cleanOptional(req.styleCode());
        if (styleCode.isBlank()) {
            styleCode = "STY-" + System.currentTimeMillis();
        }
        String fabric = cleanOptional(req.fabric());
        String design = cleanOptional(req.design());
        String imageUrl = cleanImageUrl(req.imageUrl());
        double cost = Math.max(0, req.cost() == null ? 0 : req.cost());
        int stock = Math.max(0, req.stock() == null ? 0 : req.stock());
        List<String> colors = cleanVariants(req.colors(), "");
        List<String> sizes = cleanVariants(req.sizes(), "—");
        long base = System.currentTimeMillis();

        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setSize(req.size() == null || req.size().isBlank() ? "—" : req.size());
        p.setColor(req.color() == null ? "" : req.color().trim());
        p.setImageUrl(cleanImageUrl(req.imageUrl()));
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
        if (req.imageUrl() != null) {
            p.setImageUrl(cleanImageUrl(req.imageUrl()));
        }
        return products.save(p);
    }

    @Transactional
    public void delete(String sku) {
        Product p = get(sku);
        products.delete(p);
    }

    private String cleanImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return "";
        }
        String trimmed = imageUrl.trim();
        if (trimmed.startsWith("https://") || trimmed.startsWith("http://") || trimmed.startsWith("data:image/")) {
            return trimmed;
        }
        throw new IllegalArgumentException("Product image must be an http(s) URL or data:image URI");
    }
}
