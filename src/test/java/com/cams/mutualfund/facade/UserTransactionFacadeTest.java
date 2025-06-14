package com.cams.mutualfund.facade;

import com.cams.mutualfund.data.Roles;
import com.cams.mutualfund.data.TransactionType;
import com.cams.mutualfund.data.dao.*;
import com.cams.mutualfund.data.dto.UserHoldingDTO;
import com.cams.mutualfund.data.dto.UserPortfolioDTO;
import com.cams.mutualfund.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserTransactionFacadeTest {

    @Mock
    private IUserService userService;

    @Mock
    private IScriptService scriptService;

    @Mock
    private INavService navService;

    @Mock
    private IUserHoldingService userHoldingService;

    @Mock
    private ITransactionService transactionService;

    @InjectMocks
    private UserTransactionFacade userTransactionFacade;

    private CamsUser testUser;
    private Script testScript;
    private Nav todayNav;
    private UserHolding existingHolding;
    private final Long userId = 1L;
    private final Long scriptId = 1L;
    private final double navValue = 25.0;
    private final double amount = 1000.0;
    private final double units = amount / navValue; // 40.0

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set up test data
        testUser = new CamsUser("testuser", "password", Roles.USER);
        testUser.setId(userId);

        testScript = new Script("TEST123", "Test Fund", "Equity", "Test AMC");
        testScript.setId(scriptId);

        todayNav = new Nav(LocalDate.now(), navValue, testScript);
        todayNav.setId(1L);

        existingHolding = new UserHolding();
        existingHolding.setCamsUser(testUser);
        existingHolding.setScript(testScript);
        existingHolding.setUnits(100.0);
        existingHolding.setTotalValue(100.0 * navValue);
        existingHolding.setId(1L);

        // Default mock behavior
        when(userService.findUser(userId)).thenReturn(testUser);
        when(scriptService.findScript(scriptId)).thenReturn(testScript);
        when(navService.findTodayNav(testScript)).thenReturn(todayNav);
    }

    @Test
    void buyUnits_WithValidInputs_ShouldBuyUnits() {
        // Arrange
        // Create a copy of the holding to simulate the behavior of updating units
        UserHolding holdingCopy = new UserHolding();
        holdingCopy.setId(existingHolding.getId());
        holdingCopy.setCamsUser(existingHolding.getCamsUser());
        holdingCopy.setScript(existingHolding.getScript());
        holdingCopy.setUnits(existingHolding.getUnits());
        holdingCopy.setTotalValue(existingHolding.getTotalValue());
        
        when(userHoldingService.getOrCreateUserHolding(testUser, testScript)).thenReturn(holdingCopy);
        
        // Act
        userTransactionFacade.buyUnits(userId, scriptId, amount);

        // Assert
        verify(userService).findUser(userId);
        verify(scriptService).findScript(scriptId);
        verify(navService).findTodayNav(testScript);
        verify(userHoldingService).getOrCreateUserHolding(testUser, testScript);
        
        // Verify the holding was updated with the correct units
        // The facade updates the holding object directly before passing it to updateHolding
        ArgumentCaptor<UserHolding> holdingCaptor = ArgumentCaptor.forClass(UserHolding.class);
        verify(userHoldingService).updateHolding(holdingCaptor.capture(), eq(navValue));
        
        UserHolding updatedHolding = holdingCaptor.getValue();
        assertEquals(100.0 + units, updatedHolding.getUnits(), 0.001);
        
        // Verify transaction was recorded with exact parameters
        ArgumentCaptor<Double> unitsCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> amountCaptor = ArgumentCaptor.forClass(Double.class);
        
        verify(transactionService).recordTransaction(
            eq(testUser), 
            eq(testScript), 
            eq(TransactionType.BUY), 
            unitsCaptor.capture(), 
            amountCaptor.capture(), 
            eq(navValue)
        );
        
        assertEquals(units, unitsCaptor.getValue(), 0.001);
        assertEquals(amount, amountCaptor.getValue(), 0.001);
    }

    @Test
    void redeemUnits_WithValidInputs_ShouldRedeemUnits() {
        // Arrange
        double unitsToRedeem = 50.0;
        double redemptionAmount = unitsToRedeem * navValue;
        
        // Create a copy of the holding to simulate the behavior of updating units
        UserHolding holdingCopy = new UserHolding();
        holdingCopy.setId(existingHolding.getId());
        holdingCopy.setCamsUser(existingHolding.getCamsUser());
        holdingCopy.setScript(existingHolding.getScript());
        holdingCopy.setUnits(existingHolding.getUnits());
        holdingCopy.setTotalValue(existingHolding.getTotalValue());
        
        when(userHoldingService.getUserHolding(testUser, testScript)).thenReturn(holdingCopy);
        doNothing().when(userHoldingService).validateSufficientUnits(holdingCopy, unitsToRedeem);
        
        // Act
        userTransactionFacade.redeemUnits(userId, scriptId, unitsToRedeem);

        // Assert
        verify(userService).findUser(userId);
        verify(scriptService).findScript(scriptId);
        verify(navService).findTodayNav(testScript);
        verify(userHoldingService).getUserHolding(testUser, testScript);
        verify(userHoldingService).validateSufficientUnits(holdingCopy, unitsToRedeem);
        
        // Verify the holding was updated with the correct units
        // The facade updates the holding object directly before passing it to updateHolding
        ArgumentCaptor<UserHolding> holdingCaptor = ArgumentCaptor.forClass(UserHolding.class);
        verify(userHoldingService).updateHolding(holdingCaptor.capture(), eq(navValue));
        
        UserHolding updatedHolding = holdingCaptor.getValue();
        assertEquals(100.0 - unitsToRedeem, updatedHolding.getUnits(), 0.001);
        
        // Verify transaction was recorded with exact parameters
        ArgumentCaptor<Double> unitsCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> amountCaptor = ArgumentCaptor.forClass(Double.class);
        
        verify(transactionService).recordTransaction(
            eq(testUser), 
            eq(testScript), 
            eq(TransactionType.REDEEM), 
            unitsCaptor.capture(), 
            amountCaptor.capture(), 
            eq(navValue)
        );
        
        assertEquals(unitsToRedeem, unitsCaptor.getValue(), 0.001);
        assertEquals(redemptionAmount, amountCaptor.getValue(), 0.001);
    }

    @Test
    void getUserPortfolio_WithHoldings_ShouldReturnPortfolio() {
        // Arrange
        double currentNavValue = 30.0; // Current NAV is higher than invested NAV
        
        when(userService.findUser(userId)).thenReturn(testUser);
        when(userHoldingService.findByCamsUser(testUser)).thenReturn(List.of(existingHolding));
        when(navService.getLatestNavValue(testScript)).thenReturn(currentNavValue);
        
        // Act
        UserPortfolioDTO portfolio = userTransactionFacade.getUserPortfolio(userId);
        
        // Assert
        assertNotNull(portfolio);
        assertEquals(userId, portfolio.userId());
        assertEquals(testUser.getUsername(), portfolio.username());
        assertEquals(1, portfolio.holdings().size());
        
        // Calculate expected values
        double investedValue = existingHolding.getTotalValue();
        double currentValue = existingHolding.getUnits() * currentNavValue;
        double profitLoss = currentValue - investedValue;
        double profitLossPercentage = (profitLoss / investedValue) * 100;
        
        // Verify portfolio totals
        assertEquals(investedValue, portfolio.totalInvestedValue(), 0.001);
        assertEquals(currentValue, portfolio.totalCurrentValue(), 0.001);
        assertEquals(profitLoss, portfolio.totalProfitLoss(), 0.001);
        assertEquals(profitLossPercentage, portfolio.totalProfitLossPercentage(), 0.001);
        
        // Verify holding details
        UserHoldingDTO holdingDTO = portfolio.holdings().get(0);
        assertEquals(existingHolding.getId(), holdingDTO.holdingId());
        assertEquals(testScript.getId(), holdingDTO.scriptId());
        assertEquals(testScript.getFundCode(), holdingDTO.fundCode());
        assertEquals(existingHolding.getUnits(), holdingDTO.units());
        assertEquals(currentNavValue, holdingDTO.currentNavValue());
        assertEquals(currentValue, holdingDTO.currentTotalValue(), 0.001);
        assertEquals(investedValue, holdingDTO.investedValue(), 0.001);
        assertEquals(profitLoss, holdingDTO.profitLoss(), 0.001);
        assertEquals(profitLossPercentage, holdingDTO.profitLossPercentage(), 0.001);
        
        // Verify service calls
        verify(userService).findUser(userId);
        verify(userHoldingService).findByCamsUser(testUser);
        verify(navService).getLatestNavValue(testScript);
    }
    
    @Test
    void getUserPortfolio_WithNoHoldings_ShouldReturnEmptyPortfolio() {
        // Arrange
        when(userService.findUser(userId)).thenReturn(testUser);
        when(userHoldingService.findByCamsUser(testUser)).thenReturn(Collections.emptyList());
        
        // Act
        UserPortfolioDTO portfolio = userTransactionFacade.getUserPortfolio(userId);
        
        // Assert
        assertNotNull(portfolio);
        assertEquals(userId, portfolio.userId());
        assertEquals(testUser.getUsername(), portfolio.username());
        assertTrue(portfolio.holdings().isEmpty());
        assertEquals(0.0, portfolio.totalInvestedValue());
        assertEquals(0.0, portfolio.totalCurrentValue());
        assertEquals(0.0, portfolio.totalProfitLoss());
        assertEquals(0.0, portfolio.totalProfitLossPercentage());
        
        // Verify service calls
        verify(userService).findUser(userId);
        verify(userHoldingService).findByCamsUser(testUser);
        verify(navService, never()).getLatestNavValue(any());
    }
    
    @Test
    void buyUnits_WithUserNotFound_ShouldPropagateException() {
        // Arrange
        when(userService.findUser(userId)).thenThrow(new RuntimeException("CamsUser not found"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userTransactionFacade.buyUnits(userId, scriptId, amount)
        );
        
        assertEquals("CamsUser not found", exception.getMessage());
        verify(userService).findUser(userId);
        verify(scriptService, never()).findScript(any());
        verify(navService, never()).findTodayNav(any());
        verify(userHoldingService, never()).getOrCreateUserHolding(any(), any());
        verify(userHoldingService, never()).updateHolding(any(), anyDouble());
        verify(transactionService, never()).recordTransaction(any(), any(), any(), anyDouble(), anyDouble(), anyDouble());
    }
    
    @Test
    void redeemUnits_WithInsufficientUnits_ShouldPropagateException() {
        // Arrange
        double unitsToRedeem = 150.0; // More than available
        
        when(userHoldingService.getUserHolding(testUser, testScript)).thenReturn(existingHolding);
        doThrow(new IllegalArgumentException("Insufficient units to redeem. Available: 100.0"))
            .when(userHoldingService).validateSufficientUnits(existingHolding, unitsToRedeem);
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userTransactionFacade.redeemUnits(userId, scriptId, unitsToRedeem)
        );
        
        assertTrue(exception.getMessage().contains("Insufficient units"));
        verify(userService).findUser(userId);
        verify(scriptService).findScript(scriptId);
        verify(userHoldingService).getUserHolding(testUser, testScript);
        verify(userHoldingService).validateSufficientUnits(existingHolding, unitsToRedeem);
        verify(userHoldingService, never()).updateHolding(any(), anyDouble());
        verify(transactionService, never()).recordTransaction(any(), any(), any(), anyDouble(), anyDouble(), anyDouble());
    }
}
