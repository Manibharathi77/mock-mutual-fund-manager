package com.cams.mutualfund.service;

import com.cams.mutualfund.data.TransactionType;
import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.data.dao.Script;
import com.cams.mutualfund.data.dao.Transaction;
import com.cams.mutualfund.repository.TransactionRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class TransactionService implements ITransactionService {

    private static final Logger logger = LogManager.getLogger(TransactionService.class);
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Record a transaction in the system
     * 
     * @param camsUser The user making the transaction
     * @param script The script being transacted
     * @param type The type of transaction (BUY or REDEEM)
     * @param units The number of units involved
     * @param amount The monetary amount involved
     * @param navValue The NAV value at the time of transaction
     * @return The saved transaction
     */
    @Override
    public Transaction recordTransaction(CamsUser camsUser, Script script, TransactionType type,
                                   double units, double amount, double navValue) {
        Transaction transaction = new Transaction();
        transaction.setCamsUser(camsUser);
        transaction.setScript(script);
        transaction.setType(type);
        transaction.setUnits(units);
        transaction.setAmount(amount);
        transaction.setNavValue(navValue);
        transaction.setTransactionDate(LocalDate.now());

        Transaction savedTxn = transactionRepository.save(transaction);
        logger.debug("Transaction recorded with ID: {}", savedTxn.getId());
        return savedTxn;
    }
}
