package com.bharathisilks.service;

import com.bharathisilks.domain.Category;
import com.bharathisilks.error.NotFoundException;
import com.bharathisilks.repo.CategoryRepository;
import com.bharathisilks.repo.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final AuditService audit;

    public CategoryService(CategoryRepository categories, ProductRepository products, AuditService audit) {
        this.categories = categories;
        this.products = products;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<String> names() {
        return categories.findAllByOrderByNameAsc().stream().map(Category::getName).toList();
    }

    /** Ensures the standard categories exist; safe to call repeatedly. */
    @Transactional
    public List<String> seedDefaults() {
        for (String n : RetailRules.CATEGORIES) {
            ensure(n);
        }
        return names();
    }

    @Transactional
    public void resetToDefaults() {
        categories.deleteAll();
        seedDefaults();
    }

    /** Returns the category, creating it (with a derived SKU prefix) if new. */
    @Transactional
    public Category ensure(String rawName) {
        String name = (rawName == null || rawName.isBlank()) ? "Other" : rawName.trim();
        return categories.findById(name).orElseGet(() -> {
            Category c = new Category();
            c.setName(name);
            c.setPrefix(uniquePrefix(name));
            return categories.save(c);
        });
    }

    @Transactional
    public Category add(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name is required");
        }
        boolean isNew = categories.findById(name.trim()).isEmpty();
        Category c = ensure(name);
        if (isNew) {
            audit.record("category.add", "category", c.getName(), "prefix " + c.getPrefix());
        }
        return c;
    }

    @Transactional
    public void delete(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        Category c = categories.findById(name)
                .orElseThrow(() -> new NotFoundException("No category " + name));
        if ("Other".equalsIgnoreCase(name)) {
            throw new IllegalArgumentException("The default 'Other' category can't be removed");
        }
        if (products.existsByCategory(name)) {
            throw new IllegalArgumentException("Move or remove products in '" + name + "' first");
        }
        categories.delete(c);
        audit.record("category.delete", "category", name, "");
    }

    private String uniquePrefix(String name) {
        if (RetailRules.CATEGORIES.contains(name)) {
            return RetailRules.prefixFor(name);
        }
        String letters = name.replaceAll("[^A-Za-z]", "").toUpperCase();
        String base = letters.length() >= 3 ? letters.substring(0, 3) : (letters + "XXX").substring(0, 3);
        String prefix = base;
        int n = 1;
        while (categories.existsByPrefix(prefix)) {
            prefix = base.substring(0, 2) + n;
            n++;
            if (n > 99) {
                prefix = base + (System.nanoTime() % 97);
                break;
            }
        }
        return prefix;
    }
}
