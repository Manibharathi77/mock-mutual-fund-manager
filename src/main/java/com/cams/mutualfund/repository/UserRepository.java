package com.cams.mutualfund.repository;

import com.cams.mutualfund.data.dao.CamsUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<CamsUser, Long> {
    Optional<CamsUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
