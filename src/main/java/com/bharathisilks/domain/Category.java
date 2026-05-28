package com.bharathisilks.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A product category. The {@code prefix} drives SKU generation (e.g. SAR -> BS-SAR-0007). */
@Entity
@Table(name = "categories")
@Getter
@Setter
public class Category {

    @Id
    private String name;

    @Column(nullable = false)
    private String prefix;
}
