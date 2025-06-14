package com.cams.mutualfund.service;

import com.cams.mutualfund.data.Roles;
import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.data.dao.Nav;
import com.cams.mutualfund.data.dao.Script;
import com.cams.mutualfund.data.dto.UserDTO;
import com.cams.mutualfund.data.request.CreateScriptRequest;
import com.cams.mutualfund.data.request.UserRegistrationRequest;
import com.cams.mutualfund.exceptions.DuplicateUsernameException;
import com.cams.mutualfund.repository.NavRepository;
import com.cams.mutualfund.repository.ScriptRepository;
import com.cams.mutualfund.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
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
    private Script testScript;
    private CamsUser testUser;
    private UserRegistrationRequest validUserRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        validRequest = new CreateScriptRequest(
                "TEST123",
                "Test Fund",
                "Equity",
                "Test AMC"
        );

        testScript = new Script(
                "TEST123",
                "Test Fund",
                "Equity",
                "Test AMC"
        );
        testScript.setId(1L);

        testUser = new CamsUser("testuser", "encodedPassword", Roles.USER);
        testUser.setId(1L);

        validUserRequest = new UserRegistrationRequest(
                "newuser",
                "password123",
                "USER"
        );

        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
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

    // New tests for addNavForToday method

    @Test
    void addNavForToday_WithValidData_ShouldAddNav() {
        // Arrange
        String fundCode = "TEST123";
        Double navValue = 25.75;
        LocalDate today = LocalDate.now();

        when(scriptRepository.findByFundCode(fundCode)).thenReturn(Optional.of(testScript));
        when(navRepository.findByScriptAndDate(any(Script.class), any(LocalDate.class))).thenReturn(Optional.empty());

        // Act
        adminService.addNavForToday(fundCode, navValue);

        // Assert
        verify(scriptRepository).findByFundCode(fundCode);
        verify(navRepository).findByScriptAndDate(eq(testScript), eq(today));
        verify(navRepository).save(argThat(nav ->
                nav.getScript().equals(testScript) &&
                        nav.getDate().equals(today) &&
                        nav.getNavValue().equals(navValue)
        ));
    }

    @Test
    void addNavForToday_WithNonExistentScript_ShouldThrowException() {
        // Arrange
        String fundCode = "NONEXISTENT";
        Double navValue = 25.75;

        when(scriptRepository.findByFundCode(fundCode)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.addNavForToday(fundCode, navValue)
        );

        assertEquals("Script not found", exception.getMessage());
        verify(scriptRepository).findByFundCode(fundCode);
        verify(navRepository, never()).findByScriptAndDate(any(), any());
        verify(navRepository, never()).save(any());
    }

    @Test
    void addNavForToday_WithExistingNavForToday_ShouldThrowException() {
        // Arrange
        String fundCode = "TEST123";
        Double navValue = 25.75;
        LocalDate today = LocalDate.now();
        Nav existingNav = new Nav(today, 24.50, testScript);

        when(scriptRepository.findByFundCode(fundCode)).thenReturn(Optional.of(testScript));
        when(navRepository.findByScriptAndDate(testScript, today)).thenReturn(Optional.of(existingNav));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adminService.addNavForToday(fundCode, navValue)
        );

        assertEquals("NAV for today already exists", exception.getMessage());
        verify(scriptRepository).findByFundCode(fundCode);
        verify(navRepository).findByScriptAndDate(testScript, today);
        verify(navRepository, never()).save(any());
    }

    // New tests for getAllUsers method

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        // Arrange
        CamsUser user1 = new CamsUser("user1", "password1", Roles.USER);
        CamsUser user2 = new CamsUser("user2", "password2", Roles.ADMIN);
        List<CamsUser> expectedUsers = Arrays.asList(user1, user2);

        when(userRepository.findAll()).thenReturn(expectedUsers);

        // Act
        List<CamsUser> result = adminService.getAllUsers();

        // Assert
        assertEquals(expectedUsers.size(), result.size());
        assertEquals(expectedUsers, result);
        verify(userRepository).findAll();
    }

    @Test
    void getAllUsers_WithNoUsers_ShouldReturnEmptyList() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of());

        // Act
        List<CamsUser> result = adminService.getAllUsers();

        // Assert
        assertTrue(result.isEmpty());
        verify(userRepository).findAll();
    }

    // New tests for registerUser method

    @Test
    void registerUser_WithValidRequest_ShouldRegisterUser() {
        // Arrange
        when(userRepository.findByUsername(validUserRequest.username())).thenReturn(Optional.empty());

        CamsUser savedUser = new CamsUser(
                validUserRequest.username(),
                "encodedPassword",
                Roles.valueOf(validUserRequest.role())
        );
        savedUser.setId(2L);

        when(userRepository.save(any(CamsUser.class))).thenReturn(savedUser);

        // Act
        UserDTO result = adminService.registerUser(validUserRequest);

        // Assert
        assertNotNull(result);
        assertEquals(savedUser.getId(), result.id());
        assertEquals(validUserRequest.username(), result.username());
        assertEquals(validUserRequest.role(), result.role());

        verify(userRepository).findByUsername(validUserRequest.username());
        verify(passwordEncoder).encode(validUserRequest.password());
        verify(userRepository).save(any(CamsUser.class));
    }

    @Test
    void registerUser_WithExistingUsername_ShouldThrowDuplicateUsernameException() {
        // Arrange
        when(userRepository.findByUsername(validUserRequest.username())).thenReturn(Optional.of(testUser));

        // Act & Assert
        DuplicateUsernameException exception = assertThrows(
                DuplicateUsernameException.class,
                () -> adminService.registerUser(validUserRequest)
        );

        assertEquals("Username 'newuser' already exists", exception.getMessage());
        verify(userRepository).findByUsername(validUserRequest.username());
        verify(userRepository, never()).save(any());
    }

    // New tests for deleteUser method

    @Test
    void deleteUser_WithExistingUser_ShouldDeleteUser() {
        // Arrange
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);

        // Act
        adminService.deleteUser(userId);

        // Assert
        verify(userRepository).existsById(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    void deleteUser_WithNonExistentUser_ShouldThrowException() {
        // Arrange
        Long userId = 999L;
        when(userRepository.existsById(userId)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.deleteUser(userId)
        );

        assertEquals("CamsUser not found", exception.getMessage());
        verify(userRepository).existsById(userId);
        verify(userRepository, never()).deleteById(any());
    }
}