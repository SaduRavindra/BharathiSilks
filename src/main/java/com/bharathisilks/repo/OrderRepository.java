package com.bharathisilks.repo;

import com.bharathisilks.domain.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByRef(String ref);

    List<Order> findAllByOrderByDateDesc();
}
