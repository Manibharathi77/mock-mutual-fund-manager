package com.cams.mutualfund.repository;

import com.cams.mutualfund.data.dao.Script;
import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.data.dao.UserHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserHoldingRepository extends JpaRepository<UserHolding, Long> {
    Optional<UserHolding> findByCamsUserAndScript(CamsUser camsUser, Script script);
    List<UserHolding> findByCamsUser(CamsUser camsUser);
}
