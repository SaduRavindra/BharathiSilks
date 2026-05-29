package com.bharathisilks.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A single status change in an order's tracking timeline. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class OrderEvent {

    private String status;
    private long at;

    public OrderEvent(String status, long at) {
        this.status = status;
        this.at = at;
    }
}
