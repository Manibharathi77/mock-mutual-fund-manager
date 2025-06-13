package com.cams.mutualfund.service;

import com.cams.mutualfund.data.request.CreateScriptRequest;
import com.cams.mutualfund.data.dao.Script;
import com.cams.mutualfund.repository.ScriptRepository;
import com.cams.mutualfund.repository.NavRepository;
import com.cams.mutualfund.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AdminServiceTest {

    @Mock
    private ScriptRepository scriptRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private NavRepository navRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    private CreateScriptRequest validRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        validRequest = new CreateScriptRequest(
            "TEST123",
            "Test Fund",
            "Equity",
            "Test AMC"
        );
    }

    @Test
    void createScript_WithValidRequest_ShouldCreateScript() {
        // Arrange
        Script expectedScript = new Script(
            validRequest.fundCode(),
            validRequest.name(),
            validRequest.category(),
            validRequest.amc()
        );
        expectedScript.setId(1L);

        when(scriptRepository.findByFundCode(anyString())).thenReturn(Optional.empty());
        when(scriptRepository.save(any(Script.class))).thenReturn(expectedScript);

        // Act
        Script result = adminService.createScript(validRequest);

        // Assert
        assertNotNull(result);
        assertEquals(expectedScript.getId(), result.getId());
        assertEquals(validRequest.fundCode(), result.getFundCode());
        assertEquals(validRequest.name(), result.getName());
        
        verify(scriptRepository, times(1)).findByFundCode(validRequest.fundCode());
        verify(scriptRepository, times(1)).save(any(Script.class));
    }

    @Test
    void createScript_WithDuplicateFundCode_ShouldThrowException() {
        // Arrange
        Script existingScript = new Script(
            validRequest.fundCode(),
            "Existing Fund",
            "Debt",
            "Existing AMC"
        );
        
        when(scriptRepository.findByFundCode(validRequest.fundCode()))
            .thenReturn(Optional.of(existingScript));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> adminService.createScript(validRequest)
        );
        
        assertEquals("Script with fund code already exists", exception.getMessage());
        verify(scriptRepository, times(1)).findByFundCode(validRequest.fundCode());
        verify(scriptRepository, never()).save(any(Script.class));
    }

    @Test
    void createScript_WithNullRequest_ShouldThrowException() {
        // Act & Assert
        assertThrows(
            NullPointerException.class,
            () -> adminService.createScript(null)
        );
        
        verify(scriptRepository, never()).findByFundCode(anyString());
        verify(scriptRepository, never()).save(any(Script.class));
    }

    @Test
    void createScript_WithEmptyFundCode_ShouldThrowException() {
        // Arrange
        CreateScriptRequest request = new CreateScriptRequest(
            "", // Empty fund code
            "Test Fund",
            "Equity",
            "Test AMC"
        );
        
        // Mock the repository to throw an exception when save is called with empty fund code
        when(scriptRepository.save(any(Script.class))).thenThrow(
            new IllegalArgumentException("Fund code cannot be empty")
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> adminService.createScript(request)
        );
        
        assertTrue(exception.getMessage().contains("Fund code cannot be empty"));
        verify(scriptRepository).findByFundCode("");
    }

    @Test
    void createScript_WithNullFields_ShouldThrowException() {
        // Arrange
        CreateScriptRequest request = new CreateScriptRequest(
            "TEST123",
            null, // Null name
            null, // Null category
            null  // Null AMC
        );
        
        // Mock the repository to throw an exception when save is called with null fields
        when(scriptRepository.save(any(Script.class))).thenThrow(
            new IllegalArgumentException("Required fields must not be null")
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> adminService.createScript(request)
        );
        
        assertTrue(exception.getMessage().contains("must not be null"));
        verify(scriptRepository).findByFundCode("TEST123");
    }
}