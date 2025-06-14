package com.cams.mutualfund.service;

import com.cams.mutualfund.data.Roles;
import com.cams.mutualfund.data.TransactionType;
import com.cams.mutualfund.data.dao.*;
import com.cams.mutualfund.data.dto.UserHoldingDTO;
import com.cams.mutualfund.data.dto.UserPortfolioDTO;
import com.cams.mutualfund.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserTransactionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ScriptRepository scriptRepository;

    @Mock
    private NavRepository navRepository;

    @Mock
    private UserHoldingRepository userHoldingRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private UserTransactionService userTransactionService;

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
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(scriptRepository.findById(scriptId)).thenReturn(Optional.of(testScript));
        when(navRepository.findByScriptAndDate(eq(testScript), any(LocalDate.class))).thenReturn(Optional.of(todayNav));
    }

    @Test
    void buyUnits_WithValidInputs_ShouldBuyUnits() {
        // Arrange
        when(userHoldingRepository.findByCamsUserAndScript(testUser, testScript)).thenReturn(Optional.of(existingHolding));
        
        // Make a copy of the existing holding to avoid modifying the original
        UserHolding holdingCopy = new UserHolding();
        holdingCopy.setId(existingHolding.getId());
        holdingCopy.setCamsUser(existingHolding.getCamsUser());
        holdingCopy.setScript(existingHolding.getScript());
        holdingCopy.setUnits(existingHolding.getUnits());
        holdingCopy.setTotalValue(existingHolding.getTotalValue());
        
        when(userHoldingRepository.findByCamsUserAndScript(testUser, testScript)).thenReturn(Optional.of(holdingCopy));
        
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction savedTransaction = invocation.getArgument(0);
            savedTransaction.setId(1L); // Set an ID for the saved transaction
            return savedTransaction;
        });
        
        // Act
        userTransactionService.buyUnits(userId, scriptId, amount);

        // Assert
        verify(userRepository).findById(userId);
        verify(scriptRepository).findById(scriptId);
        verify(navRepository).findByScriptAndDate(eq(testScript), any(LocalDate.class));
        verify(userHoldingRepository).findByCamsUserAndScript(testUser, testScript);

        // Verify holding was updated
        ArgumentCaptor<UserHolding> holdingCaptor = ArgumentCaptor.forClass(UserHolding.class);
        verify(userHoldingRepository).save(holdingCaptor.capture());
        UserHolding updatedHolding = holdingCaptor.getValue();
        
        // Print debug values
        System.out.println("Original units: " + existingHolding.getUnits());
        System.out.println("Units to add: " + units);
        System.out.println("Expected units: " + (existingHolding.getUnits() + units));
        System.out.println("Actual units: " + updatedHolding.getUnits());
        System.out.println("Expected total value: " + (existingHolding.getUnits() + units) * navValue);
        System.out.println("Actual total value: " + updatedHolding.getTotalValue());
        
        assertEquals(existingHolding.getUnits() + units, updatedHolding.getUnits());
        assertEquals((existingHolding.getUnits() + units) * navValue, updatedHolding.getTotalValue(), 0.001);

        // Verify transaction was recorded
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        
        assertEquals(testUser, savedTransaction.getCamsUser());
        assertEquals(testScript, savedTransaction.getScript());
        assertEquals(TransactionType.BUY, savedTransaction.getType());
        assertEquals(units, savedTransaction.getUnits());
        assertEquals(amount, savedTransaction.getAmount());
        assertEquals(navValue, savedTransaction.getNavValue());
    }

    @Test
    void buyUnits_WithNewHolding_ShouldCreateHolding() {
        // Arrange
        when(userHoldingRepository.findByCamsUserAndScript(testUser, testScript)).thenReturn(Optional.empty());
        when(userHoldingRepository.save(any(UserHolding.class))).thenAnswer(invocation -> {
            UserHolding savedHolding = invocation.getArgument(0);
            savedHolding.setId(2L);
            return savedHolding;
        });
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction savedTransaction = invocation.getArgument(0);
            savedTransaction.setId(1L); // Set an ID for the saved transaction
            return savedTransaction;
        });

        // Act
        userTransactionService.buyUnits(userId, scriptId, amount);

        // Assert
        verify(userHoldingRepository).findByCamsUserAndScript(testUser, testScript);

        // Verify new holding was created and saved
        ArgumentCaptor<UserHolding> holdingCaptor = ArgumentCaptor.forClass(UserHolding.class);
        verify(userHoldingRepository).save(holdingCaptor.capture());
        UserHolding savedHolding = holdingCaptor.getValue();
        
        assertEquals(testUser, savedHolding.getCamsUser());
        assertEquals(testScript, savedHolding.getScript());
        assertEquals(units, savedHolding.getUnits());
        assertEquals(units * navValue, savedHolding.getTotalValue());

        // Verify transaction was recorded
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        
        assertEquals(testUser, savedTransaction.getCamsUser());
        assertEquals(testScript, savedTransaction.getScript());
        assertEquals(TransactionType.BUY, savedTransaction.getType());
        assertEquals(units, savedTransaction.getUnits());
        assertEquals(amount, savedTransaction.getAmount());
        assertEquals(navValue, savedTransaction.getNavValue());
    }

    @Test
    void buyUnits_WithUserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userTransactionService.buyUnits(userId, scriptId, amount)
        );

        assertEquals("CamsUser not found", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(scriptRepository, never()).findById(any());
        verify(navRepository, never()).findByScriptAndDate(any(), any());
        verify(userHoldingRepository, never()).findByCamsUserAndScript(any(), any());
        verify(userHoldingRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void redeemUnits_WithValidInputs_ShouldRedeemUnits() {
        // Arrange
        double unitsToRedeem = 50.0;
        double redemptionAmount = unitsToRedeem * navValue;

        when(userHoldingRepository.findByCamsUserAndScript(testUser, testScript)).thenReturn(Optional.of(existingHolding));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction savedTransaction = invocation.getArgument(0);
            savedTransaction.setId(1L); // Set an ID for the saved transaction
            return savedTransaction;
        });
        
        // Make a copy of the existing holding to avoid modifying the original
        UserHolding holdingCopy = new UserHolding();
        holdingCopy.setId(existingHolding.getId());
        holdingCopy.setCamsUser(existingHolding.getCamsUser());
        holdingCopy.setScript(existingHolding.getScript());
        holdingCopy.setUnits(existingHolding.getUnits());
        holdingCopy.setTotalValue(existingHolding.getTotalValue());
        
        when(userHoldingRepository.findByCamsUserAndScript(testUser, testScript)).thenReturn(Optional.of(holdingCopy));
        
        // Act
        userTransactionService.redeemUnits(userId, scriptId, unitsToRedeem);

        // Assert
        verify(userRepository).findById(userId);
        verify(scriptRepository).findById(scriptId);
        verify(navRepository).findByScriptAndDate(eq(testScript), any(LocalDate.class));
        verify(userHoldingRepository).findByCamsUserAndScript(testUser, testScript);

        // Verify holding was updated
        ArgumentCaptor<UserHolding> holdingCaptor = ArgumentCaptor.forClass(UserHolding.class);
        verify(userHoldingRepository).save(holdingCaptor.capture());
        UserHolding updatedHolding = holdingCaptor.getValue();
        
        // Print debug values
        System.out.println("Original units: " + existingHolding.getUnits());
        System.out.println("Units to redeem: " + unitsToRedeem);
        System.out.println("Expected units: " + (existingHolding.getUnits() - unitsToRedeem));
        System.out.println("Actual units: " + updatedHolding.getUnits());
        System.out.println("Expected total value: " + updatedHolding.getUnits() * navValue);
        System.out.println("Actual total value: " + updatedHolding.getTotalValue());
        
        // The units should be the original units minus the redeemed units
        assertEquals(existingHolding.getUnits() - unitsToRedeem, updatedHolding.getUnits());
        
        // The total value should be the updated units times the NAV value
        assertEquals(updatedHolding.getUnits() * navValue, updatedHolding.getTotalValue(), 0.001); // Using delta for double comparison

        // Verify transaction was recorded
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(testUser, savedTransaction.getCamsUser());
        assertEquals(testScript, savedTransaction.getScript());
        assertEquals(TransactionType.REDEEM, savedTransaction.getType());
        assertEquals(unitsToRedeem, savedTransaction.getUnits());
        assertEquals(redemptionAmount, savedTransaction.getAmount());
        assertEquals(navValue, savedTransaction.getNavValue());
    }

    @Test
    void redeemUnits_WithInsufficientUnits_ShouldThrowException() {
        // Arrange
        double unitsToRedeem = 150.0; // More than available

        when(userHoldingRepository.findByCamsUserAndScript(testUser, testScript)).thenReturn(Optional.of(existingHolding));

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userTransactionService.redeemUnits(userId, scriptId, unitsToRedeem)
        );

        assertTrue(exception.getMessage().contains("Insufficient units"));
        verify(userHoldingRepository).findByCamsUserAndScript(testUser, testScript);
        verify(userHoldingRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }


    @Test
    void getUserPortfolio_WithMultipleHoldings_ShouldReturnPortfolio() {
        // Arrange
        Script secondScript = new Script("TEST456", "Second Fund", "Debt", "Test AMC 2");
        secondScript.setId(2L);

        UserHolding secondHolding = new UserHolding();
        secondHolding.setCamsUser(testUser);
        secondHolding.setScript(secondScript);
        secondHolding.setUnits(50.0);
        secondHolding.setTotalValue(50.0 * 20.0); // Invested at NAV 20.0
        secondHolding.setId(2L);

        List<UserHolding> userHoldings = Arrays.asList(existingHolding, secondHolding);
        
        // Current NAV values (different from invested NAV to test profit/loss calculation)
        Nav latestNav1 = new Nav(LocalDate.now(), 30.0, testScript); // NAV increased to 30.0
        Nav latestNav2 = new Nav(LocalDate.now(), 18.0, secondScript); // NAV decreased to 18.0

        // Mock repository calls
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userHoldingRepository.findByCamsUser(testUser)).thenReturn(userHoldings);
        
        // Mock script repository calls for both scripts
        when(scriptRepository.findById(testScript.getId())).thenReturn(Optional.of(testScript));
        when(scriptRepository.findById(secondScript.getId())).thenReturn(Optional.of(secondScript));
        
        when(navRepository.findByScriptAndDate(eq(testScript), any(LocalDate.class))).thenReturn(Optional.of(latestNav1));
        when(navRepository.findByScriptAndDate(eq(secondScript), any(LocalDate.class))).thenReturn(Optional.of(latestNav2));

        // Act
        UserPortfolioDTO result = userTransactionService.getUserPortfolio(userId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.userId());
        assertEquals(testUser.getUsername(), result.username());
        assertEquals(2, result.holdings().size());

        // Calculate expected values
        double holding1InvestedValue = existingHolding.getTotalValue();
        double holding1CurrentValue = existingHolding.getUnits() * 30.0;
        double holding1ProfitLoss = holding1CurrentValue - holding1InvestedValue;
        double holding1ProfitLossPercentage = (holding1ProfitLoss / holding1InvestedValue) * 100;

        double holding2InvestedValue = secondHolding.getTotalValue();
        double holding2CurrentValue = secondHolding.getUnits() * 18.0;
        double holding2ProfitLoss = holding2CurrentValue - holding2InvestedValue;
        double holding2ProfitLossPercentage = (holding2ProfitLoss / holding2InvestedValue) * 100;

        double totalInvestedValue = holding1InvestedValue + holding2InvestedValue;
        double totalCurrentValue = holding1CurrentValue + holding2CurrentValue;
        double totalProfitLoss = totalCurrentValue - totalInvestedValue;
        double totalProfitLossPercentage = (totalProfitLoss / totalInvestedValue) * 100;

        // Verify first holding
        UserHoldingDTO firstHoldingDTO = result.holdings().get(0);
        assertEquals(existingHolding.getId(), firstHoldingDTO.holdingId());
        assertEquals(testScript.getId(), firstHoldingDTO.scriptId());
        assertEquals(testScript.getFundCode(), firstHoldingDTO.fundCode());
        assertEquals(existingHolding.getUnits(), firstHoldingDTO.units());
        assertEquals(30.0, firstHoldingDTO.currentNavValue());
        assertEquals(holding1CurrentValue, firstHoldingDTO.currentTotalValue());
        assertEquals(holding1InvestedValue, firstHoldingDTO.investedValue());
        assertEquals(holding1ProfitLoss, firstHoldingDTO.profitLoss());
        assertEquals(holding1ProfitLossPercentage, firstHoldingDTO.profitLossPercentage());

        // Verify second holding
        UserHoldingDTO secondHoldingDTO = result.holdings().get(1);
        assertEquals(secondHolding.getId(), secondHoldingDTO.holdingId());
        assertEquals(secondScript.getId(), secondHoldingDTO.scriptId());
        assertEquals(secondScript.getFundCode(), secondHoldingDTO.fundCode());
        assertEquals(secondHolding.getUnits(), secondHoldingDTO.units());
        assertEquals(18.0, secondHoldingDTO.currentNavValue());
        assertEquals(holding2CurrentValue, secondHoldingDTO.currentTotalValue());
        assertEquals(holding2InvestedValue, secondHoldingDTO.investedValue());
        assertEquals(holding2ProfitLoss, secondHoldingDTO.profitLoss());
        assertEquals(holding2ProfitLossPercentage, secondHoldingDTO.profitLossPercentage());

        // Verify portfolio totals
        assertEquals(totalInvestedValue, result.totalInvestedValue());
        assertEquals(totalCurrentValue, result.totalCurrentValue());
        assertEquals(totalProfitLoss, result.totalProfitLoss());
        assertEquals(totalProfitLossPercentage, result.totalProfitLossPercentage());

        // Verify repository calls
        verify(userRepository).findById(userId);
        verify(userHoldingRepository).findByCamsUser(testUser);
        verify(navRepository, times(2)).findByScriptAndDate(any(Script.class), any(LocalDate.class));
    }

    @Test
    void getUserPortfolio_WithNoHoldings_ShouldReturnEmptyPortfolio() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userHoldingRepository.findByCamsUser(testUser)).thenReturn(Collections.emptyList());

        // Act
        UserPortfolioDTO result = userTransactionService.getUserPortfolio(userId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.userId());
        assertEquals(testUser.getUsername(), result.username());
        assertTrue(result.holdings().isEmpty());
        assertEquals(0.0, result.totalInvestedValue());
        assertEquals(0.0, result.totalCurrentValue());
        assertEquals(0.0, result.totalProfitLoss());
        assertEquals(0.0, result.totalProfitLossPercentage());

        // Verify repository calls
        verify(userRepository).findById(userId);
        verify(userHoldingRepository).findByCamsUser(testUser);
        verify(navRepository, never()).findByScriptAndDate(any(Script.class), any(LocalDate.class));
    }

    @Test
    void getUserPortfolio_WithUserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userTransactionService.getUserPortfolio(userId)
        );

        assertEquals("CamsUser not found", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userHoldingRepository, never()).findByCamsUser(any());
        verify(navRepository, never()).findByScriptAndDate(any(), any());
    }
}
