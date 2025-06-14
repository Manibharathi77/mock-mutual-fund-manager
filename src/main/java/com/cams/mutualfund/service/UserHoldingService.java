package com.cams.mutualfund.service;

import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.data.dao.Script;
import com.cams.mutualfund.data.dao.UserHolding;
import com.cams.mutualfund.repository.UserHoldingRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class UserHoldingService implements IUserHoldingService {

    private static final Logger logger = LogManager.getLogger(UserHoldingService.class);
    private final UserHoldingRepository userHoldingRepository;

    public UserHoldingService(UserHoldingRepository userHoldingRepository) {
        this.userHoldingRepository = userHoldingRepository;
    }

    /**
     * Find a user's holding for a specific script
     * 
     * @param camsUser The user
     * @param script The script
     * @return The user's holding for the script
     * @throws RuntimeException if the holding is not found
     */
    @Override
    public UserHolding getUserHolding(CamsUser camsUser, Script script) {
        logger.debug("Checking user holdings for script: {}", script.getFundCode());
        return userHoldingRepository.findByCamsUserAndScript(camsUser, script)
                .orElseThrow(() -> {
                    logger.error("User {} does not hold script {}", camsUser.getUsername(), script.getFundCode());
                    return new RuntimeException("CamsUser does not hold this script");
                });
    }

    /**
     * Get or create a user's holding for a specific script
     * 
     * @param camsUser The user
     * @param script The script
     * @return The existing or newly created user holding
     */
    @Override
    public UserHolding getOrCreateUserHolding(CamsUser camsUser, Script script) {
        logger.debug("Checking if user already holds this script");
        return userHoldingRepository.findByCamsUserAndScript(camsUser, script)
                .orElseGet(() -> {
                    logger.debug("Creating new holding for user: {} and script: {}", camsUser.getUsername(), script.getFundCode());
                    UserHolding newHolding = new UserHolding();
                    newHolding.setCamsUser(camsUser);
                    newHolding.setScript(script);
                    newHolding.setUnits(0.0);
                    newHolding.setTotalValue(0.0);
                    return newHolding;
                });
    }

    /**
     * Update a user holding with the current NAV value
     * 
     * @param holding The holding to update
     * @param navValue The current NAV value
     * @return The updated holding
     */
    @Override
    public UserHolding updateHolding(UserHolding holding, double navValue) {
        holding.setTotalValue(holding.getUnits() * navValue);
        holding.setLastUpdated(LocalDate.now());

        UserHolding savedHolding = userHoldingRepository.save(holding);
        logger.debug("Updated user holding - Units: {}, Value: {}", holding.getUnits(), holding.getTotalValue());
        return savedHolding;
    }

    /**
     * Find all holdings for a user
     * 
     * @param camsUser The user
     * @return List of all holdings for the user
     */
    @Override
    public List<UserHolding> findByCamsUser(CamsUser camsUser) {
        return userHoldingRepository.findByCamsUser(camsUser);
    }

    /**
     * Validate that a user has sufficient units for redemption
     * 
     * @param holding The user's holding
     * @param unitsToRedeem The number of units to redeem
     * @throws IllegalArgumentException if the user has insufficient units
     */
    @Override
    public void validateSufficientUnits(UserHolding holding, double unitsToRedeem) {
        if (holding.getUnits() < unitsToRedeem) {
            logger.warn("Redemption failed: Insufficient units. Requested: {}, Available: {}",
                    unitsToRedeem, holding.getUnits());
            throw new IllegalArgumentException("Insufficient units to redeem. Available: " + holding.getUnits());
        }
    }
}
