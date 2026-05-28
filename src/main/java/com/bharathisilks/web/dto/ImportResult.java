package com.bharathisilks.web.dto;

import com.bharathisilks.domain.Product;
import java.util.List;

/** Outcome of a bulk import: how many rows landed, and the affected products. */
public record ImportResult(int created, int updated, int skipped, List<Product> products) {
}
