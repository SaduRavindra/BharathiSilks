package com.bharathisilks.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A person who can sign in to the console (via Google or phone OTP). */
@Entity
@Table(name = "app_users")
@Getter
@Setter
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    /** Stable principal, e.g. {@code google:<sub>} or {@code phone:<digits>}. */
    @Column(unique = true, nullable = false)
    private String subject;

    private String name;
    private String email;
    private String phone;

    @Column(length = 512)
    private String picture;

    private String provider;
    private String role;
    private long created;
}
