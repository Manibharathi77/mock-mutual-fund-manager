package com.cams.mutualfund.repository;

import com.cams.mutualfund.data.dao.Script;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScriptRepository extends JpaRepository<Script, Long> {
    Optional<Script> findByFundCode(String fundCode);
}
