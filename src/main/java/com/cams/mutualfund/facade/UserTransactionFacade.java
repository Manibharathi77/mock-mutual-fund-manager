package com.cams.mutualfund.facade;

import com.cams.mutualfund.data.TransactionType;
import com.cams.mutualfund.data.dao.*;
import com.cams.mutualfund.data.dto.UserHoldingDTO;
import com.cams.mutualfund.data.dto.UserPortfolioDTO;
import com.cams.mutualfund.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Facade that coordinates transaction operations across multiple services.
 * This class delegates repository operations to dedicated service classes.
 */
@Service
public class UserTransactionFacade implements IUserTransactionFacade {

    private static final Logger logger = LogManager.getLogger(UserTransactionFacade.class);

    private final IUserService userService;
    private final IScriptService scriptService;
    private final INavService navService;
    private final IUserHoldingService userHoldingService;
    private final ITransactionService transactionService;

    public UserTransactionFacade(IUserService userService, 
                                IScriptService scriptService, 
                                INavService navService, 
                                IUserHoldingService userHoldingService, 
                                ITransactionService transactionService) {
        this.userService = userService;
        this.scriptService = scriptService;
        this.navService = navService;
        this.userHoldingService = userHoldingService;
        this.transactionService = transactionService;
    }

    // NOTE: Using REPEATABLE_READ since it is a banking operation and need for high consistency.
    // But avoiding serializable isolation level to improve performance.
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void buyUnits(Long userId, Long scriptId, Double amount) {
        logger.info("Processing buy transaction - User ID: {}, Script ID: {}, Amount: {}", userId, scriptId, amount);

        TransactionContext context = prepareTransactionContext(userId, scriptId);
        double units = calculateUnits(amount, context.nav.getNavValue());

        UserHolding holding = userHoldingService.getOrCreateUserHolding(context.camsUser, context.script);

        holding.setUnits(holding.getUnits() + units);

        userHoldingService.updateHolding(holding, context.nav.getNavValue());
        transactionService.recordTransaction(context.camsUser, context.script, TransactionType.BUY, units, amount, context.nav.getNavValue());

        logger.info("Buy transaction completed successfully - Units: {}", units);
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void redeemUnits(Long userId, Long scriptId, double unitsToRedeem) {
        logger.info("Processing redemption transaction - User ID: {}, Script ID: {}, Units: {}", userId, scriptId, unitsToRedeem);

        TransactionContext context = prepareTransactionContext(userId, scriptId);

        UserHolding holding = userHoldingService.getUserHolding(context.camsUser, context.script);
        userHoldingService.validateSufficientUnits(holding, unitsToRedeem);
        holding.setUnits(holding.getUnits() - unitsToRedeem);
        double redemptionAmount = calculateAmount(unitsToRedeem, context.nav.getNavValue());

        userHoldingService.updateHolding(holding, context.nav.getNavValue());
        transactionService.recordTransaction(context.camsUser, context.script, TransactionType.REDEEM,
                unitsToRedeem, redemptionAmount, context.nav.getNavValue());

        logger.info("Redemption transaction completed successfully - Amount: {}", redemptionAmount);
    }

    private record TransactionContext(CamsUser camsUser, Script script, Nav nav) {
    }

    private TransactionContext prepareTransactionContext(Long userId, Long scriptId) {
        CamsUser camsUser = userService.findUser(userId);
        Script script = scriptService.findScript(scriptId);
        Nav nav = navService.findTodayNav(script);

        return new TransactionContext(camsUser, script, nav);
    }

    private double calculateUnits(double amount, double navValue) {
        double units = amount / navValue;
        logger.debug("Calculated units: {} at NAV: {}", units, navValue);
        return units;
    }

    private double calculateAmount(double units, double navValue) {
        double amount = units * navValue;
        logger.debug("Calculated amount: {} for units: {} at NAV: {}", amount, units, navValue);
        return amount;
    }

    @Override
    public UserPortfolioDTO getUserPortfolio(Long userId) {
        logger.info("Retrieving portfolio for user ID: {}", userId);
        CamsUser camsUser = userService.findUser(userId);
        List<UserHolding> holdings = userHoldingService.findByCamsUser(camsUser);
        
        List<UserHoldingDTO> holdingDTOs = new ArrayList<>();
        double totalInvestedValue = 0.0;
        double totalCurrentValue = 0.0;
        
        for (UserHolding holding : holdings) {
            double currentNavValue = navService.getLatestNavValue(holding.getScript());
            double currentValue = holding.getUnits() * currentNavValue;
            double investedValue = holding.getTotalValue();
            double profitLoss = currentValue - investedValue;
            double profitLossPercentage = (investedValue > 0) ? (profitLoss / investedValue) * 100 : 0;
            
            UserHoldingDTO holdingDTO = new UserHoldingDTO(
                    holding.getId(),
                    holding.getScript().getId(),
                    holding.getScript().getFundCode(),
                    holding.getScript().getName(),
                    holding.getScript().getCategory(),
                    holding.getScript().getAmc(),
                    holding.getUnits(),
                    currentNavValue,
                    currentValue,
                    investedValue,
                    profitLoss,
                    profitLossPercentage
            );
            
            holdingDTOs.add(holdingDTO);
            totalInvestedValue += investedValue;
            totalCurrentValue += currentValue;
        }
        
        double totalProfitLoss = totalCurrentValue - totalInvestedValue;
        double totalProfitLossPercentage = (totalInvestedValue > 0) ? (totalProfitLoss / totalInvestedValue) * 100 : 0;
        
        UserPortfolioDTO portfolioDTO = new UserPortfolioDTO(
                userId,
                camsUser.getUsername(),
                holdingDTOs,
                totalInvestedValue,
                totalCurrentValue,
                totalProfitLoss,
                totalProfitLossPercentage
        );
        
        logger.info("Portfolio retrieved successfully for user: {}", camsUser.getUsername());
        return portfolioDTO;
    }
}
