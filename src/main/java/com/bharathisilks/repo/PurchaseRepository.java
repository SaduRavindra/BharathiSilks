package com.bharathisilks.repo;

import com.bharathisilks.domain.Purchase;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findAllByOrderByDateAsc();
}
