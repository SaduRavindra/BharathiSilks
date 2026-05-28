package com.bharathisilks.web.dto;

import java.util.List;

/** Bulk product creation for one parent style across color/size variants. */
public record VariantMatrixRequest(
        String name,
        String category,
        String styleCode,
        String fabric,
        String design,
        List<String> colors,
        List<String> sizes,
        String imageUrl,
        Double cost,
        Double price,
        Integer stock) {
}
