package com.cams.mutualfund.repository;

import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.data.dao.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCamsUser(CamsUser camsUser);
}
