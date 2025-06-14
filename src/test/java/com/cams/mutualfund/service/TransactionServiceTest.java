package com.cams.mutualfund.service;

import com.cams.mutualfund.data.TransactionType;
import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.data.dao.Script;
import com.cams.mutualfund.data.dao.Transaction;
import com.cams.mutualfund.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;
    
    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    private CamsUser testUser;
    private Script testScript;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up test user
        testUser = new CamsUser();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        
        // Set up test script
        testScript = new Script();
        testScript.setId(1L);
        testScript.setFundCode("TEST001");
        testScript.setName("Test Fund");
        
        // Set up test transaction
        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setCamsUser(testUser);
        testTransaction.setScript(testScript);
        testTransaction.setType(TransactionType.BUY);
        testTransaction.setUnits(10.0);
        testTransaction.setAmount(250.0);
        testTransaction.setNavValue(25.0);
        testTransaction.setTransactionDate(LocalDate.now());
    }

    @Test
    void recordTransaction_BuyTransaction_ShouldSaveAndReturnTransaction() {
        // Arrange
        double units = 10.0;
        double amount = 250.0;
        double navValue = 25.0;
        
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        
        // Act
        Transaction result = transactionService.recordTransaction(
                testUser, testScript, TransactionType.BUY, units, amount, navValue);
        
        // Assert
        assertNotNull(result);
        assertEquals(testTransaction.getId(), result.getId());
        
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction capturedTransaction = transactionCaptor.getValue();
        
        assertEquals(testUser, capturedTransaction.getCamsUser());
        assertEquals(testScript, capturedTransaction.getScript());
        assertEquals(TransactionType.BUY, capturedTransaction.getType());
        assertEquals(units, capturedTransaction.getUnits());
        assertEquals(amount, capturedTransaction.getAmount());
        assertEquals(navValue, capturedTransaction.getNavValue());
        assertEquals(LocalDate.now(), capturedTransaction.getTransactionDate());
    }

    @Test
    void recordTransaction_RedeemTransaction_ShouldSaveAndReturnTransaction() {
        // Arrange
        double units = 5.0;
        double amount = 125.0;
        double navValue = 25.0;
        
        testTransaction.setType(TransactionType.REDEEM);
        testTransaction.setUnits(units);
        testTransaction.setAmount(amount);
        
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        
        // Act
        Transaction result = transactionService.recordTransaction(
                testUser, testScript, TransactionType.REDEEM, units, amount, navValue);
        
        // Assert
        assertNotNull(result);
        assertEquals(testTransaction.getId(), result.getId());
        
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction capturedTransaction = transactionCaptor.getValue();
        
        assertEquals(testUser, capturedTransaction.getCamsUser());
        assertEquals(testScript, capturedTransaction.getScript());
        assertEquals(TransactionType.REDEEM, capturedTransaction.getType());
        assertEquals(units, capturedTransaction.getUnits());
        assertEquals(amount, capturedTransaction.getAmount());
        assertEquals(navValue, capturedTransaction.getNavValue());
        assertEquals(LocalDate.now(), capturedTransaction.getTransactionDate());
    }
}
