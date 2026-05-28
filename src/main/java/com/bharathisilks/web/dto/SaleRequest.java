package com.bharathisilks.web.dto;

import java.util.List;

/**
 * A checkout request. Prices, tax, totals and loyalty are all computed
 * server-side from the current product records — the client only sends
 * which SKUs and how many, plus the chosen discount/payment options.
 */
public record SaleRequest(
        List<Line> items,
        String phone,
        String name,
        Double disc,
        String discType,
        String pay,
        Boolean redeem) {

    public record Line(String sku, Integer qty) {
    }
}
