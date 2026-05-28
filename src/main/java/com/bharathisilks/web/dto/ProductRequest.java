package com.bharathisilks.web.dto;

/** Payload for creating a product. SKU and GST are assigned by the server. */
public record ProductRequest(
        String name,
        String category,
        String size,
        String color,
        String imageUrl,
        Double cost,
        Double price,
        Integer stock) {
}
