package com.bharathisilks.web.dto;

import com.bharathisilks.domain.Sale;
import java.util.List;
import java.util.Map;

/** Analytics for a trailing window of {@code days}. */
public record ReportResponse(
        int days,
        double revenue,
        double profit,
        double tax,
        int bills,
        double avgBill,
        List<BestSeller> best,
        Map<String, Double> paymentMix,
        List<DeadStock> deadStock,
        List<Sale> recent) {

    public record BestSeller(String sku, String name, int qty, double amt) {
    }

    public record DeadStock(String sku, String name, int stock, double value) {
    }
}
