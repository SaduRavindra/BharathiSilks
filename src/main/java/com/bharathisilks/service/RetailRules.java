package com.bharathisilks.service;

import java.util.List;
import java.util.Map;

/** Business constants and rules shared with the storefront UI. */
public final class RetailRules {

    private RetailRules() {
    }

    /** Stock at or below this count is flagged "low". */
    public static final int LOW_STOCK = 3;

    /** One loyalty point is earned per this many rupees spent. */
    public static final int POINTS_PER = 200;

    public static final List<String> CATEGORIES =
            List.of("Sarees", "Lehengas", "Dresses", "Kurtis", "Blouses", "Other");

    private static final Map<String, String> PREFIX = Map.of(
            "Sarees", "SAR",
            "Lehengas", "LEH",
            "Dresses", "DRS",
            "Kurtis", "KUR",
            "Blouses", "BLO",
            "Other", "OTH");

    public static String prefixFor(String category) {
        return PREFIX.getOrDefault(category, "OTH");
    }

    /** Indian apparel GST: 5% up to Rs.1000, 12% above. */
    public static int gstRate(double price) {
        return price > 1000 ? 12 : 5;
    }
}
