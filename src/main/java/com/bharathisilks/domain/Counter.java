package com.bharathisilks.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Per-category running number used to mint sequential SKUs. */
@Entity
@Table(name = "counters")
@Getter
@Setter
public class Counter {

    @Id
    private String prefix;

    // "value" is a reserved word in H2/SQL, so map to a safe column name.
    @Column(name = "counter_value")
    private int value;
}
