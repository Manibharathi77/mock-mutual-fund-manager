package com.cams.mutualfund.service;

import com.cams.mutualfund.data.dao.Nav;
import com.cams.mutualfund.data.dao.Script;
import com.cams.mutualfund.repository.NavRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NavServiceTest {

    @Mock
    private NavRepository navRepository;

    @InjectMocks
    private NavService navService;

    private Script testScript;
    private Nav testNav;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up test script
        testScript = new Script();
        testScript.setId(1L);
        testScript.setFundCode("TEST001");
        testScript.setName("Test Fund");
        
        // Set up test NAV
        testNav = new Nav();
        testNav.setId(1L);
        testNav.setScript(testScript);
        testNav.setNavValue(25.75);
        testNav.setDate(today);
    }

    @Test
    void findTodayNav_WhenNavExists_ShouldReturnNav() {
        // Arrange
        when(navRepository.findByScriptAndDate(eq(testScript), eq(today)))
            .thenReturn(Optional.of(testNav));
        
        // Act
        Nav result = navService.findTodayNav(testScript);
        
        // Assert
        assertNotNull(result);
        assertEquals(testNav.getId(), result.getId());
        assertEquals(testNav.getNavValue(), result.getNavValue());
        assertEquals(today, result.getDate());
        verify(navRepository, times(1)).findByScriptAndDate(testScript, today);
    }

    @Test
    void findTodayNav_WhenNavDoesNotExist_ShouldThrowException() {
        // Arrange
        when(navRepository.findByScriptAndDate(eq(testScript), eq(today)))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            navService.findTodayNav(testScript);
        });
        
        assertEquals("NAV not found for today", exception.getMessage());
        verify(navRepository, times(1)).findByScriptAndDate(testScript, today);
    }

    @Test
    void getLatestNavValue_WhenNavExists_ShouldReturnNavValue() {
        // Arrange
        when(navRepository.findByScriptAndDate(eq(testScript), eq(today)))
            .thenReturn(Optional.of(testNav));
        
        // Act
        Double result = navService.getLatestNavValue(testScript);
        
        // Assert
        assertEquals(25.75, result);
        verify(navRepository, times(1)).findByScriptAndDate(testScript, today);
    }

    @Test
    void getLatestNavValue_WhenNavDoesNotExist_ShouldThrowException() {
        // Arrange
        when(navRepository.findByScriptAndDate(eq(testScript), eq(today)))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            navService.getLatestNavValue(testScript);
        });
        
        assertTrue(exception.getMessage().contains("Latest NAV not found for script"));
        verify(navRepository, times(1)).findByScriptAndDate(testScript, today);
    }
}
