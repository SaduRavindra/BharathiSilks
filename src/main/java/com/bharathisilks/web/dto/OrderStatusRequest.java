package com.bharathisilks.web.dto;

/** Staff update to an order's fulfilment status. */
public record OrderStatusRequest(String status, String note) {
}
