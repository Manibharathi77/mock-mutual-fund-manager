package com.cams.mutualfund.service;

import com.cams.mutualfund.data.TransactionType;
import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.data.dao.Script;
import com.cams.mutualfund.data.dao.Transaction;

/**
 * Interface for transaction-related operations
 */
public interface ITransactionService {
    
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
    Transaction recordTransaction(CamsUser camsUser, Script script, TransactionType type,
                               double units, double amount, double navValue);
}
