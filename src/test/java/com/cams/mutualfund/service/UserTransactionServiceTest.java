package com.cams.mutualfund.service;

import com.cams.mutualfund.data.Roles;
import com.cams.mutualfund.data.TransactionType;
import com.cams.mutualfund.data.dao.*;
import com.cams.mutualfund.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        
        // Act
        userTransactionService.buyUnits(userId, scriptId, amount);

        // Assert
        verify(userRepository).findById(userId);
        verify(scriptRepository).findById(scriptId);
        verify(navRepository).findByScriptAndDate(eq(testScript), any(LocalDate.class));
        verify(userHoldingRepository).findByCamsUserAndScript(testUser, testScript);
        
        // Verify holding was updated
        verify(userHoldingRepository).save(argThat(holding -> 
            holding.getUnits() == existingHolding.getUnits() + units &&
            holding.getTotalValue() == (existingHolding.getUnits() + units) * navValue
        ));
        
        // Verify transaction was recorded
        verify(transactionRepository).save(argThat(transaction -> 
            transaction.getCamsUser().equals(testUser) &&
            transaction.getScript().equals(testScript) &&
            transaction.getType() == TransactionType.BUY &&
            transaction.getUnits() == units &&
            transaction.getAmount() == amount &&
            transaction.getNavValue() == navValue
        ));
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
        
        // Act
        userTransactionService.buyUnits(userId, scriptId, amount);

        // Assert
        verify(userHoldingRepository).findByCamsUserAndScript(testUser, testScript);
        
        // Verify new holding was created and saved
        verify(userHoldingRepository).save(argThat(holding -> 
            holding.getCamsUser().equals(testUser) &&
            holding.getScript().equals(testScript) &&
            holding.getUnits() == units &&
            holding.getTotalValue() == units * navValue
        ));
        
        // Verify transaction was recorded
        verify(transactionRepository).save(any(Transaction.class));
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
        
        // Act
        userTransactionService.redeemUnits(userId, scriptId, unitsToRedeem);

        // Assert
        verify(userRepository).findById(userId);
        verify(scriptRepository).findById(scriptId);
        verify(navRepository).findByScriptAndDate(eq(testScript), any(LocalDate.class));
        verify(userHoldingRepository).findByCamsUserAndScript(testUser, testScript);
        
        // Verify holding was updated
        verify(userHoldingRepository).save(argThat(holding -> 
            holding.getUnits() == existingHolding.getUnits() - unitsToRedeem &&
            holding.getTotalValue() == (existingHolding.getUnits() - unitsToRedeem) * navValue
        ));
        
        // Verify transaction was recorded
        verify(transactionRepository).save(argThat(transaction -> 
            transaction.getCamsUser().equals(testUser) &&
            transaction.getScript().equals(testScript) &&
            transaction.getType() == TransactionType.REDEEM &&
            transaction.getUnits() == unitsToRedeem &&
            transaction.getAmount() == redemptionAmount &&
            transaction.getNavValue() == navValue
        ));
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
    void redeemUnits_WithNegativeUnits_ShouldThrowException() {
        // Arrange
        double unitsToRedeem = -10.0;
        
        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userTransactionService.redeemUnits(userId, scriptId, unitsToRedeem)
        );
        
        assertTrue(exception.getMessage().contains("Units must be positive"));
        verify(userHoldingRepository, never()).findByCamsUserAndScript(any(), any());
        verify(userHoldingRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}
