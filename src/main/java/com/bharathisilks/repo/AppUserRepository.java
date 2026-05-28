package com.bharathisilks.repo;

import com.bharathisilks.domain.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findBySubject(String subject);
}
