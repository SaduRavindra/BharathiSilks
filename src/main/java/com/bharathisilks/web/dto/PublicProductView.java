package com.bharathisilks.web.dto;

/** Customer-facing product fields only — never exposes cost. */
public record PublicProductView(
        String sku,
        String name,
        String category,
        String size,
        String color,
        double price,
        boolean inStock) {
}
