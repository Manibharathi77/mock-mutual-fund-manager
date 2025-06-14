package com.cams.mutualfund.service;

import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.data.dao.Script;
import com.cams.mutualfund.data.dao.UserHolding;

import java.util.List;

/**
 * Interface for user holding-related operations
 */
public interface IUserHoldingService {
    
    /**
     * Find a user's holding for a specific script
     * 
     * @param camsUser The user
     * @param script The script
     * @return The user's holding for the script
     * @throws RuntimeException if the holding is not found
     */
    UserHolding getUserHolding(CamsUser camsUser, Script script);
    
    /**
     * Get or create a user's holding for a specific script
     * 
     * @param camsUser The user
     * @param script The script
     * @return The existing or newly created user holding
     */
    UserHolding getOrCreateUserHolding(CamsUser camsUser, Script script);
    
    /**
     * Update a user holding with the current NAV value
     * 
     * @param holding The holding to update
     * @param navValue The current NAV value
     * @return The updated holding
     */
    UserHolding updateHolding(UserHolding holding, double navValue);
    
    /**
     * Find all holdings for a user
     * 
     * @param camsUser The user
     * @return List of all holdings for the user
     */
    List<UserHolding> findByCamsUser(CamsUser camsUser);
    
    /**
     * Validate that a user has sufficient units for redemption
     * 
     * @param holding The user's holding
     * @param unitsToRedeem The number of units to redeem
     * @throws IllegalArgumentException if the user has insufficient units
     */
    void validateSufficientUnits(UserHolding holding, double unitsToRedeem);
}
