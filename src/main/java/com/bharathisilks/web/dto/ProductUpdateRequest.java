package com.bharathisilks.web.dto;

/** Editable product fields from the inventory screen. GST is recomputed from price. */
public record ProductUpdateRequest(
        Double price,
        Integer stock) {
}
