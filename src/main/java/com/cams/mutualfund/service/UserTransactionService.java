package com.cams.mutualfund.service;

import com.cams.mutualfund.data.TransactionType;
import com.cams.mutualfund.data.dao.*;
import com.cams.mutualfund.repository.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class UserTransactionService {

    private static final Logger logger = LogManager.getLogger(UserTransactionService.class);

    private final UserRepository userRepository;
    private final ScriptRepository scriptRepository;
    private final NavRepository navRepository;
    private final UserHoldingRepository userHoldingRepository;
    private final TransactionRepository transactionRepository;

    public UserTransactionService(UserRepository userRepository, ScriptRepository scriptRepository, NavRepository navRepository, UserHoldingRepository userHoldingRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.scriptRepository = scriptRepository;
        this.navRepository = navRepository;
        this.userHoldingRepository = userHoldingRepository;
        this.transactionRepository = transactionRepository;
    }

    // NOTE: Using REPEATABLE_READ since it is a banking operation and need for high consistency.
    // But avoiding serializable isolation level to improve performance.
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void buyUnits(Long userId, Long scriptId, Double amount) {
        logger.info("Processing buy transaction - User ID: {}, Script ID: {}, Amount: {}", userId, scriptId, amount);

        TransactionContext context = prepareTransactionContext(userId, scriptId);
        double units = calculateUnits(amount, context.nav.getNavValue());

        UserHolding holding = getOrCreateUserHolding(context.camsUser, context.script);

        holding.setUnits(holding.getUnits() + units);

        updateHolding(holding, context.nav.getNavValue());
        recordTransaction(context.camsUser, context.script, TransactionType.BUY, units, amount, context.nav.getNavValue());

        logger.info("Buy transaction completed successfully - Units: {}", units);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void redeemUnits(Long userId, Long scriptId, double unitsToRedeem) {
        logger.info("Processing redemption transaction - User ID: {}, Script ID: {}, Units: {}", userId, scriptId, unitsToRedeem);

        validatePositiveUnits(unitsToRedeem);
        TransactionContext context = prepareTransactionContext(userId, scriptId);

        UserHolding holding = getUserHolding(context.camsUser, context.script);
        validateSufficientUnits(holding, unitsToRedeem);
        holding.setUnits(holding.getUnits() - unitsToRedeem);
        double redemptionAmount = calculateAmount(unitsToRedeem, context.nav.getNavValue());

        updateHolding(holding, context.nav.getNavValue());
        recordTransaction(context.camsUser, context.script, TransactionType.REDEEM,
                unitsToRedeem, redemptionAmount, context.nav.getNavValue());

        logger.info("Redemption transaction completed successfully - Amount: {}", redemptionAmount);
    }

    // Helper class to hold common transaction context
    private record TransactionContext(CamsUser camsUser, Script script, Nav nav) {
    }

    // Common method to prepare transaction context
    private TransactionContext prepareTransactionContext(Long userId, Long scriptId) {
        CamsUser camsUser = findUser(userId);
        Script script = findScript(scriptId);
        Nav nav = findTodayNav(script);

        return new TransactionContext(camsUser, script, nav);
    }

    // Find user by ID
    private CamsUser findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("Transaction failed: User not found with ID: {}", userId);
                    return new RuntimeException("CamsUser not found");
                });
    }

    // Find script by ID
    private Script findScript(Long scriptId) {
        return scriptRepository.findById(scriptId)
                .orElseThrow(() -> {
                    logger.error("Transaction failed: Script not found with ID: {}", scriptId);
                    return new RuntimeException("Script not found");
                });
    }

    // Find today's NAV for a script
    private Nav findTodayNav(Script script) {
        logger.debug("Fetching today's NAV for script: {}", script.getFundCode());
        return navRepository.findByScriptAndDate(script, LocalDate.now())
                .orElseThrow(() -> {
                    logger.error("Transaction failed: NAV not found for today for script: {}", script.getFundCode());
                    return new RuntimeException("NAV not found for today");
                });
    }

    // Calculate units from amount and NAV
    private double calculateUnits(double amount, double navValue) {
        double units = amount / navValue;
        logger.debug("Calculated units: {} at NAV: {}", units, navValue);
        return units;
    }

    // Calculate amount from units and NAV
    private double calculateAmount(double units, double navValue) {
        double amount = units * navValue;
        logger.debug("Calculated amount: {} for units: {} at NAV: {}", amount, units, navValue);
        return amount;
    }

    // Get user holding, creating if it doesn't exist
    private UserHolding getUserHolding(CamsUser camsUser, Script script) {
        logger.debug("Checking user holdings for script: {}", script.getFundCode());
        return userHoldingRepository.findByCamsUserAndScript(camsUser, script)
                .orElseThrow(() -> {
                    logger.error("Transaction failed: User {} does not hold script {}", camsUser.getUsername(), script.getFundCode());
                    return new RuntimeException("CamsUser does not hold this script");
                });
    }

    // Get or create user holding
    private UserHolding getOrCreateUserHolding(CamsUser camsUser, Script script) {
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

    // Update user holding
    private void updateHolding(UserHolding holding, double navValue) {

        holding.setTotalValue(holding.getUnits() * navValue);
        holding.setLastUpdated(LocalDate.now());

        userHoldingRepository.save(holding);
        logger.debug("Updated user holding - Units: {}, Value: {}", holding.getUnits(), holding.getTotalValue());
    }

    // Record transaction
    private void recordTransaction(CamsUser camsUser, Script script, TransactionType type,
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
    }

    // Validate that units are positive
    private void validatePositiveUnits(double units) {
        if (units <= 0) {
            logger.warn("Transaction failed: Units must be positive, received: {}", units);
            throw new IllegalArgumentException("Units must be positive");
        }
    }

    // Validate that user has sufficient units
    private void validateSufficientUnits(UserHolding holding, double unitsToRedeem) {
        if (holding.getUnits() < unitsToRedeem) {
            logger.warn("Redemption failed: Insufficient units. Requested: {}, Available: {}",
                    unitsToRedeem, holding.getUnits());
            throw new IllegalArgumentException("Insufficient units to redeem. Available: " + holding.getUnits());
        }
    }
}