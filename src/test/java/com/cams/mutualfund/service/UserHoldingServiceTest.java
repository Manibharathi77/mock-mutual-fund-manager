package com.cams.mutualfund.service;

import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.data.dao.Script;
import com.cams.mutualfund.data.dao.UserHolding;
import com.cams.mutualfund.repository.UserHoldingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserHoldingServiceTest {

    @Mock
    private UserHoldingRepository userHoldingRepository;

    @InjectMocks
    private UserHoldingService userHoldingService;
    
    @Captor
    private ArgumentCaptor<UserHolding> userHoldingCaptor;

    private CamsUser testUser;
    private Script testScript;
    private UserHolding testHolding;

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
        
        // Set up test holding
        testHolding = new UserHolding();
        testHolding.setId(1L);
        testHolding.setCamsUser(testUser);
        testHolding.setScript(testScript);
        testHolding.setUnits(10.0);
        testHolding.setTotalValue(250.0);
        testHolding.setLastUpdated(LocalDate.now());
    }

    @Test
    void getUserHolding_WhenHoldingExists_ShouldReturnHolding() {
        // Arrange
        when(userHoldingRepository.findByCamsUserAndScript(testUser, testScript))
            .thenReturn(Optional.of(testHolding));
        
        // Act
        UserHolding result = userHoldingService.getUserHolding(testUser, testScript);
        
        // Assert
        assertNotNull(result);
        assertEquals(testHolding.getId(), result.getId());
        assertEquals(testHolding.getUnits(), result.getUnits());
        assertEquals(testHolding.getTotalValue(), result.getTotalValue());
        verify(userHoldingRepository, times(1)).findByCamsUserAndScript(testUser, testScript);
    }

    @Test
    void getUserHolding_WhenHoldingDoesNotExist_ShouldThrowException() {
        // Arrange
        when(userHoldingRepository.findByCamsUserAndScript(testUser, testScript))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userHoldingService.getUserHolding(testUser, testScript);
        });
        
        assertTrue(exception.getMessage().contains("CamsUser does not hold this script"));
        verify(userHoldingRepository, times(1)).findByCamsUserAndScript(testUser, testScript);
    }

    @Test
    void getOrCreateUserHolding_WhenHoldingExists_ShouldReturnExistingHolding() {
        // Arrange
        when(userHoldingRepository.findByCamsUserAndScript(testUser, testScript))
            .thenReturn(Optional.of(testHolding));
        
        // Act
        UserHolding result = userHoldingService.getOrCreateUserHolding(testUser, testScript);
        
        // Assert
        assertNotNull(result);
        assertEquals(testHolding.getId(), result.getId());
        assertEquals(testHolding.getUnits(), result.getUnits());
        verify(userHoldingRepository, times(1)).findByCamsUserAndScript(testUser, testScript);
        verify(userHoldingRepository, never()).save(any());
    }

    @Test
    void getOrCreateUserHolding_WhenHoldingDoesNotExist_ShouldCreateNewHolding() {
        // Arrange
        when(userHoldingRepository.findByCamsUserAndScript(testUser, testScript))
            .thenReturn(Optional.empty());
        
        // Act
        UserHolding result = userHoldingService.getOrCreateUserHolding(testUser, testScript);
        
        // Assert
        assertNotNull(result);
        assertEquals(testUser, result.getCamsUser());
        assertEquals(testScript, result.getScript());
        assertEquals(0.0, result.getUnits());
        assertEquals(0.0, result.getTotalValue());
        verify(userHoldingRepository, times(1)).findByCamsUserAndScript(testUser, testScript);
    }

    @Test
    void updateHolding_ShouldUpdateTotalValueAndDate() {
        // Arrange
        double navValue = 30.0;
        when(userHoldingRepository.save(any(UserHolding.class))).thenReturn(testHolding);
        
        // Act
        UserHolding result = userHoldingService.updateHolding(testHolding, navValue);
        
        // Assert
        verify(userHoldingRepository).save(userHoldingCaptor.capture());
        UserHolding capturedHolding = userHoldingCaptor.getValue();
        
        assertEquals(testHolding.getUnits() * navValue, capturedHolding.getTotalValue());
        assertEquals(LocalDate.now(), capturedHolding.getLastUpdated());
    }

    @Test
    void findByCamsUser_ShouldReturnUserHoldings() {
        // Arrange
        UserHolding holding1 = new UserHolding();
        holding1.setId(1L);
        holding1.setCamsUser(testUser);
        
        UserHolding holding2 = new UserHolding();
        holding2.setId(2L);
        holding2.setCamsUser(testUser);
        
        List<UserHolding> expectedHoldings = Arrays.asList(holding1, holding2);
        
        when(userHoldingRepository.findByCamsUser(testUser)).thenReturn(expectedHoldings);
        
        // Act
        List<UserHolding> result = userHoldingService.findByCamsUser(testUser);
        
        // Assert
        assertEquals(2, result.size());
        assertEquals(expectedHoldings, result);
        verify(userHoldingRepository, times(1)).findByCamsUser(testUser);
    }

    @Test
    void validateSufficientUnits_WhenSufficient_ShouldNotThrowException() {
        // Arrange
        testHolding.setUnits(10.0);
        double unitsToRedeem = 5.0;
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            userHoldingService.validateSufficientUnits(testHolding, unitsToRedeem);
        });
    }

    @Test
    void validateSufficientUnits_WhenInsufficient_ShouldThrowException() {
        // Arrange
        testHolding.setUnits(10.0);
        double unitsToRedeem = 15.0;
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userHoldingService.validateSufficientUnits(testHolding, unitsToRedeem);
        });
        
        assertTrue(exception.getMessage().contains("Insufficient units to redeem"));
    }
}
