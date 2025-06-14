package com.cams.mutualfund.facade;

import com.cams.mutualfund.data.dto.UserPortfolioDTO;

/**
 * Interface for the facade that coordinates transaction operations across multiple services.
 */
public interface IUserTransactionFacade {
    
    /**
     * Buy units of a script for a user
     * 
     * @param userId The ID of the user
     * @param scriptId The ID of the script
     * @param amount The amount to invest
     */
    void buyUnits(Long userId, Long scriptId, Double amount);
    
    /**
     * Redeem units of a script for a user
     * 
     * @param userId The ID of the user
     * @param scriptId The ID of the script
     * @param unitsToRedeem The number of units to redeem
     */
    void redeemUnits(Long userId, Long scriptId, double unitsToRedeem);
    
    /**
     * Get a user's portfolio
     * 
     * @param userId The ID of the user
     * @return The user's portfolio
     */
    UserPortfolioDTO getUserPortfolio(Long userId);
}
