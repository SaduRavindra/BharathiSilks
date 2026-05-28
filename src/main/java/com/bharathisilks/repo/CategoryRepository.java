package com.bharathisilks.repo;

import com.bharathisilks.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, String> {

    List<Category> findAllByOrderByNameAsc();

    boolean existsByPrefix(String prefix);
}
