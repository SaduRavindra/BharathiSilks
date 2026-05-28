package com.bharathisilks.service;

import com.bharathisilks.domain.Category;
import com.bharathisilks.domain.Product;
import com.bharathisilks.error.NotFoundException;
import com.bharathisilks.repo.ProductRepository;
import com.bharathisilks.web.dto.ImportResult;
import com.bharathisilks.web.dto.ProductRequest;
import com.bharathisilks.web.dto.ProductUpdateRequest;
import com.bharathisilks.web.dto.VariantMatrixRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository products;
    private final SkuService skuService;
    private final CategoryService categories;
    private final AuditService audit;

    public ProductService(ProductRepository products, SkuService skuService,
                          CategoryService categories, AuditService audit) {
        this.products = products;
        this.skuService = skuService;
        this.categories = categories;
        this.audit = audit;
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
        double price = req.price() == null ? 0 : req.price();
        if (price <= 0) {
            throw new IllegalArgumentException("A valid selling price is required");
        }
        Category cat = categories.ensure(req.category());

        Product p = new Product();
        p.setName(name);
        p.setCategory(cat.getName());
        p.setStyleCode(opt(req.styleCode()));
        p.setFabric(opt(req.fabric()));
        p.setDesign(opt(req.design()));
        p.setSize(req.size() == null || req.size().isBlank() ? "—" : req.size());
        p.setColor(req.color() == null ? "" : req.color().trim());
        p.setImageUrl(cleanImageUrl(req.imageUrl()));
        p.setCost(Math.max(0, req.cost() == null ? 0 : req.cost()));
        p.setPrice(price);
        p.setStock(Math.max(0, req.stock() == null ? 0 : req.stock()));
        p.setGst(RetailRules.gstRate(price));
        p.setSku(skuService.next(cat.getPrefix()));
        p.setCreated(System.currentTimeMillis());
        Product saved = products.save(p);
        audit.record("product.create", "product", saved.getSku(), saved.getName());
        return saved;
    }

    /** Creates one product per colour x size combination of a parent style. */
    @Transactional
    public List<Product> createMatrix(VariantMatrixRequest req) {
        String name = req.name() == null ? "" : req.name().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        double price = req.price() == null ? 0 : req.price();
        if (price <= 0) {
            throw new IllegalArgumentException("A valid selling price is required");
        }
        Category cat = categories.ensure(req.category());
        String styleCode = req.styleCode() == null || req.styleCode().isBlank()
                ? "STY-" + System.currentTimeMillis() : req.styleCode().trim();
        String fabric = opt(req.fabric());
        String design = opt(req.design());
        String imageUrl = cleanImageUrl(req.imageUrl());
        double cost = Math.max(0, req.cost() == null ? 0 : req.cost());
        int stock = Math.max(0, req.stock() == null ? 0 : req.stock());
        List<String> colors = variants(req.colors(), "");
        List<String> sizes = variants(req.sizes(), "—");
        long now = System.currentTimeMillis();

        List<Product> created = new ArrayList<>();
        for (String color : colors) {
            for (String size : sizes) {
                Product p = new Product();
                p.setName(name);
                p.setCategory(cat.getName());
                p.setStyleCode(styleCode);
                p.setFabric(fabric);
                p.setDesign(design);
                p.setSize(size == null || size.isBlank() ? "—" : size);
                p.setColor(color == null ? "" : color.trim());
                p.setImageUrl(imageUrl);
                p.setCost(cost);
                p.setPrice(price);
                p.setStock(stock);
                p.setGst(RetailRules.gstRate(price));
                p.setSku(skuService.next(cat.getPrefix()));
                p.setCreated(now + created.size());
                created.add(products.save(p));
            }
        }
        audit.record("product.matrix", "product", styleCode, created.size() + " variants of " + name);
        return created;
    }

    @Transactional
    public Product update(String sku, ProductUpdateRequest req) {
        Product p = get(sku);
        StringBuilder detail = new StringBuilder();
        if (req.price() != null) {
            double price = Math.max(0, req.price());
            p.setPrice(price);
            p.setGst(RetailRules.gstRate(price));
            detail.append("price=").append(price).append(' ');
        }
        if (req.stock() != null) {
            int stock = Math.max(0, req.stock());
            p.setStock(stock);
            detail.append("stock=").append(stock).append(' ');
        }
        if (req.imageUrl() != null) {
            p.setImageUrl(cleanImageUrl(req.imageUrl()));
            detail.append("image");
        }
        Product saved = products.save(p);
        audit.record("product.update", "product", sku, detail.toString().trim());
        return saved;
    }

    @Transactional
    public void delete(String sku) {
        Product p = get(sku);
        products.delete(p);
        audit.record("product.delete", "product", sku, p.getName());
    }

    /**
     * Bulk import of normalized invoice rows. A row that matches an existing product
     * by name+size+colour tops up its stock (and refreshes cost/price); otherwise a
     * new product is created, auto-adding the category if needed. Rows with no usable
     * price are skipped.
     */
    @Transactional
    public ImportResult importProducts(List<ProductRequest> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("No rows to import");
        }
        Map<String, Product> byKey = new HashMap<>();
        for (Product p : products.findAll()) {
            byKey.put(key(p.getName(), p.getSize(), p.getColor()), p);
        }
        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<Product> affected = new ArrayList<>();
        for (ProductRequest r : rows) {
            String name = r.name() == null ? "" : r.name().trim();
            if (name.isEmpty()) {
                skipped++;
                continue;
            }
            String size = r.size() == null || r.size().isBlank() ? "—" : r.size().trim();
            String color = r.color() == null ? "" : r.color().trim();
            double cost = Math.max(0, r.cost() == null ? 0 : r.cost());
            double price = r.price() == null ? 0 : r.price();
            if (price <= 0) {
                price = cost; // invoices often list only the purchase cost
            }
            int stock = Math.max(0, r.stock() == null ? 0 : r.stock());

            Product existing = byKey.get(key(name, size, color));
            if (existing != null) {
                existing.setStock(Math.max(0, existing.getStock() + stock));
                if (cost > 0) {
                    existing.setCost(cost);
                }
                if (price > 0) {
                    existing.setPrice(price);
                    existing.setGst(RetailRules.gstRate(price));
                }
                products.save(existing);
                affected.add(existing);
                updated++;
                audit.record("product.import", "product", existing.getSku(), "updated " + name);
            } else {
                if (price <= 0) {
                    skipped++;
                    continue;
                }
                Category cat = categories.ensure(r.category());
                Product p = new Product();
                p.setName(name);
                p.setCategory(cat.getName());
                p.setStyleCode(opt(r.styleCode()));
                p.setFabric(opt(r.fabric()));
                p.setDesign(opt(r.design()));
                p.setSize(size);
                p.setColor(color);
                p.setImageUrl(cleanImageUrl(r.imageUrl()));
                p.setCost(cost);
                p.setPrice(price);
                p.setStock(stock);
                p.setGst(RetailRules.gstRate(price));
                p.setSku(skuService.next(cat.getPrefix()));
                p.setCreated(System.currentTimeMillis());
                products.save(p);
                byKey.put(key(name, size, color), p);
                affected.add(p);
                created++;
                audit.record("product.import", "product", p.getSku(), "created " + name);
            }
        }
        return new ImportResult(created, updated, skipped, affected);
    }

    private static String opt(String s) {
        return s == null ? "" : s.trim();
    }

    private static List<String> variants(List<String> in, String fallback) {
        List<String> out = new ArrayList<>();
        if (in != null) {
            for (String v : in) {
                if (v != null && !v.trim().isEmpty()) {
                    out.add(v.trim());
                }
            }
        }
        if (out.isEmpty()) {
            out.add(fallback);
        }
        return out;
    }

    private static String key(String name, String size, String color) {
        return (name == null ? "" : name.trim().toLowerCase()) + "|"
                + (size == null ? "" : size.trim().toLowerCase()) + "|"
                + (color == null ? "" : color.trim().toLowerCase());
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
