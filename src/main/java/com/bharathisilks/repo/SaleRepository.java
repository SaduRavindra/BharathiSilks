package com.bharathisilks.repo;

import com.bharathisilks.domain.Sale;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    Optional<Sale> findByInv(String inv);

    List<Sale> findAllByOrderByDateAsc();
}
