package com.cams.mutualfund.service;

import com.cams.mutualfund.data.dao.Script;
import com.cams.mutualfund.repository.ScriptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScriptServiceTest {

    @Mock
    private ScriptRepository scriptRepository;

    @InjectMocks
    private ScriptService scriptService;

    private Script testScript;
    private final Long scriptId = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up test script
        testScript = new Script();
        testScript.setId(scriptId);
        testScript.setFundCode("TEST001");
        testScript.setName("Test Fund");
        testScript.setCategory("Equity");
        testScript.setAmc("Test AMC");
    }

    @Test
    void findScript_WhenScriptExists_ShouldReturnScript() {
        // Arrange
        when(scriptRepository.findById(scriptId)).thenReturn(Optional.of(testScript));
        
        // Act
        Script result = scriptService.findScript(scriptId);
        
        // Assert
        assertNotNull(result);
        assertEquals(scriptId, result.getId());
        assertEquals("TEST001", result.getFundCode());
        assertEquals("Test Fund", result.getName());
        assertEquals("Equity", result.getCategory());
        verify(scriptRepository, times(1)).findById(scriptId);
    }

    @Test
    void findScript_WhenScriptDoesNotExist_ShouldThrowException() {
        // Arrange
        when(scriptRepository.findById(scriptId)).thenReturn(Optional.empty());
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            scriptService.findScript(scriptId);
        });
        
        assertEquals("Script not found", exception.getMessage());
        verify(scriptRepository, times(1)).findById(scriptId);
    }
}
