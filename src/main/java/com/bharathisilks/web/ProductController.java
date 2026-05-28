package com.bharathisilks.web;

import com.bharathisilks.domain.Product;
import com.bharathisilks.service.ProductService;
import com.bharathisilks.web.dto.ProductRequest;
import com.bharathisilks.web.dto.ProductUpdateRequest;
import com.bharathisilks.web.dto.VariantMatrixRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<Product> all() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@RequestBody ProductRequest req) {
        return service.create(req);
    }

    @PostMapping("/matrix")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Product> createMatrix(@RequestBody VariantMatrixRequest req) {
        return service.createMatrix(req);
    }

    @PutMapping("/{sku}")
    public Product update(@PathVariable String sku, @RequestBody ProductUpdateRequest req) {
        return service.update(sku, req);
    }

    @DeleteMapping("/{sku}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String sku) {
        service.delete(sku);
    }
}
