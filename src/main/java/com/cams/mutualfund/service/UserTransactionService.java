package com.cams.mutualfund.service;

import com.cams.mutualfund.data.TransactionType;
import com.cams.mutualfund.data.dao.*;
import com.cams.mutualfund.data.dto.UserHoldingDTO;
import com.cams.mutualfund.data.dto.UserPortfolioDTO;
import com.cams.mutualfund.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    // TODO: Create a separate entity for this request to avoid sending more arguments for this.
    // TODO: User BUILDER PATTERN to form the request, so that when number of args change, it's extensible.
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

    // Validate that user has sufficient units
    private void validateSufficientUnits(UserHolding holding, double unitsToRedeem) {
        if (holding.getUnits() < unitsToRedeem) {
            logger.warn("Redemption failed: Insufficient units. Requested: {}, Available: {}",
                    unitsToRedeem, holding.getUnits());
            throw new IllegalArgumentException("Insufficient units to redeem. Available: " + holding.getUnits());
        }
    }

    /**
     * Get the complete portfolio for a user with profit/loss information
     * 
     * @param userId The ID of the user
     * @return UserPortfolioDTO containing all holdings with profit/loss information
     */
    public UserPortfolioDTO getUserPortfolio(Long userId) {
        logger.info("Fetching portfolio for user ID: {}", userId);
        
        // Use the existing findUser method to maintain consistency
        CamsUser user = findUser(userId);
        List<UserHolding> holdings = userHoldingRepository.findByCamsUser(user);

        if (holdings.isEmpty()) {
            logger.info("No holdings found for user ID: {}", userId);
            return createEmptyPortfolio(user);
        }

        // Process holdings and calculate portfolio metrics
        PortfolioSummary summary = processHoldings(holdings);
        
        logger.info("Portfolio fetched successfully for user ID: {}", userId);
        return summary.toPortfolioDTO(user);
    }
    
    /**
     * Create an empty portfolio DTO for a user with no holdings
     */
    private UserPortfolioDTO createEmptyPortfolio(CamsUser user) {
        return new UserPortfolioDTO(
            user.getId(),
            user.getUsername(),
            List.of(),
            0.0,
            0.0,
            0.0,
            0.0
        );
    }
    
    /**
     * Process all holdings to calculate current values and profit/loss
     * 
     * @param holdings List of user holdings to process
     * @return PortfolioSummary containing processed holdings and aggregate metrics
     */
    private PortfolioSummary processHoldings(List<UserHolding> holdings) {
        List<UserHoldingDTO> holdingDTOs = new ArrayList<>();
        double totalInvestedValue = 0.0;
        double totalCurrentValue = 0.0;
        
        for (UserHolding holding : holdings) {
            HoldingValuation valuation = calculateHoldingValuation(holding);
            
            // Add to portfolio totals
            totalInvestedValue += valuation.investedValue();
            totalCurrentValue += valuation.currentValue();
            
            // Create and add the holding DTO
            holdingDTOs.add(createHoldingDTO(holding, valuation));
        }
        
        // Calculate overall profit/loss
        double totalProfitLoss = totalCurrentValue - totalInvestedValue;
        double totalProfitLossPercentage = calculatePercentage(totalProfitLoss, totalInvestedValue);
        
        return new PortfolioSummary(
            holdingDTOs,
            totalInvestedValue,
            totalCurrentValue,
            totalProfitLoss,
            totalProfitLossPercentage
        );
    }
    
    /**
     * Calculate valuation metrics for a single holding
     */
    private HoldingValuation calculateHoldingValuation(UserHolding holding) {
        Script script = holding.getScript();
        Double currentNavValue = getLatestNavValue(script.getId());
        
        // Calculate current value based on latest NAV
        double currentValue = holding.getUnits() * currentNavValue;
        
        // Get invested value from the holding
        double investedValue = holding.getTotalValue();
        
        // Calculate profit/loss
        double profitLoss = currentValue - investedValue;
        double profitLossPercentage = calculatePercentage(profitLoss, investedValue);
        
        return new HoldingValuation(
            currentNavValue,
            currentValue,
            investedValue,
            profitLoss,
            profitLossPercentage
        );
    }
    
    /**
     * Calculate percentage safely handling division by zero
     */
    private double calculatePercentage(double value, double base) {
        return base > 0 ? (value / base) * 100 : 0.0;
    }
    
    /**
     * Create a holding DTO with all required information
     */
    private UserHoldingDTO createHoldingDTO(UserHolding holding, HoldingValuation valuation) {
        Script script = holding.getScript();
        
        return new UserHoldingDTO(
            holding.getId(),
            script.getId(),
            script.getFundCode(),
            script.getName(),
            script.getCategory(),
            script.getAmc(),
            holding.getUnits(),
            valuation.currentNavValue(),
            valuation.currentValue(),
            valuation.investedValue(),
            valuation.profitLoss(),
            valuation.profitLossPercentage()
        );
    }
    
    /**
     * Record containing valuation metrics for a single holding
     */
    private record HoldingValuation(double currentNavValue, double currentValue, double investedValue,
                                    double profitLoss, double profitLossPercentage) {}
    
    /**
     * Class to hold portfolio summary information
     */
    private record PortfolioSummary(List<UserHoldingDTO> holdings, double totalInvestedValue, double totalCurrentValue,
                                    double totalProfitLoss, double totalProfitLossPercentage
    ) {
        /**
         * Convert to a DTO for the API response
         */
        UserPortfolioDTO toPortfolioDTO(CamsUser user) {
            return new UserPortfolioDTO(
                user.getId(),
                user.getUsername(),
                holdings,
                totalInvestedValue,
                totalCurrentValue,
                totalProfitLoss,
                totalProfitLossPercentage
            );
        }
    }

    /**
     * Get the latest NAV value for a script
     * 
     * @param scriptId The ID of the script
     * @return The latest NAV value
     */
    private Double getLatestNavValue(Long scriptId) {
        Script script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new EntityNotFoundException("Script not found with ID: " + scriptId));
        
        // Find the latest NAV for the script
        Optional<Nav> latestNav = navRepository.findByScriptAndDate(script, LocalDate.now());
        
        return latestNav.map(Nav::getNavValue)
                .orElseThrow(() -> new EntityNotFoundException("No NAV found for script ID: " + scriptId));
    }
}