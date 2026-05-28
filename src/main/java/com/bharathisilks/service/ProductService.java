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

        List<Product> matrix = new ArrayList<>();
        for (String color : colors) {
            for (String size : sizes) {
                Product p = buildProduct(name, category, styleCode, fabric, design,
                        size, color, imageUrl, cost, price, stock, base + matrix.size());
                matrix.add(products.save(p));
            }
        }
        return matrix;
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

    private Product buildProduct(String name, String category, String styleCode, String fabric, String design,
                                 String size, String color, String imageUrl, double cost, double price,
                                 int stock, long created) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setStyleCode(styleCode);
        p.setFabric(fabric);
        p.setDesign(design);
        p.setSize(size);
        p.setColor(color);
        p.setImageUrl(imageUrl);
        p.setCost(cost);
        p.setPrice(price);
        p.setStock(stock);
        p.setGst(RetailRules.gstRate(price));
        p.setSku(skuService.next(category));
        p.setCreated(created);
        return p;
    }

    private String cleanCategory(String category) {
        return RetailRules.CATEGORIES.contains(category) ? category : "Other";
    }

    private String cleanRequired(String value, String message) {
        String cleaned = cleanOptional(value);
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return cleaned;
    }

    private String cleanOptional(String value) {
        return value == null ? "" : value.trim();
    }

    private double cleanPrice(Double price) {
        double value = price == null ? 0 : price;
        if (value <= 0) {
            throw new IllegalArgumentException("A valid selling price is required");
        }
        return value;
    }

    private List<String> cleanVariants(List<String> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return List.of(fallback);
        }
        List<String> cleaned = values.stream()
                .map(this::cleanOptional)
                .filter(v -> !v.isBlank())
                .distinct()
                .toList();
        return cleaned.isEmpty() ? List.of(fallback) : cleaned;
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
