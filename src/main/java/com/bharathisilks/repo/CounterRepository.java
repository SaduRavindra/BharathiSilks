package com.bharathisilks.repo;

import com.bharathisilks.domain.Counter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounterRepository extends JpaRepository<Counter, String> {
}
