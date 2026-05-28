package com.bharathisilks.web.dto;

/** Customer-facing product fields only — never exposes cost. */
public record PublicProductView(
        String sku,
        String name,
        String category,
        String styleCode,
        String fabric,
        String design,
        String size,
        String color,
        String imageUrl,
        double price,
        boolean inStock) {
}
