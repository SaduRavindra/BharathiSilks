package com.bharathisilks.web.dto;

/** Stock-in from a supplier: adds quantity and optionally refreshes cost. */
public record PurchaseRequest(
        String sku,
        Integer qty,
        Double cost,
        String supplier) {
}
