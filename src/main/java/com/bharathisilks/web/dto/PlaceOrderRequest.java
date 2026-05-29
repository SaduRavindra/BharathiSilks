package com.bharathisilks.web.dto;

import java.util.List;

/** Customer-submitted order from the public storefront. */
public record PlaceOrderRequest(
        String name,
        String phone,
        String address,
        String fulfilment,
        String note,
        List<Line> items) {

    public record Line(String sku, Integer qty) {
    }
}
