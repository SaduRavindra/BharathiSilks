package com.bharathisilks.web.dto;

import java.util.List;

/** Bulk product import payload — normalized invoice rows. */
public record ImportRequest(List<ProductRequest> rows) {
}
