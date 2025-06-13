package com.cams.mutualfund.service;

import com.cams.mutualfund.data.Roles;
import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.data.dto.UserDTO;
import com.cams.mutualfund.data.request.UserRegistrationRequest;
import com.cams.mutualfund.exceptions.DuplicateUsernameException;
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

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRegistrationRequest validRequest;
    private CamsUser savedUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        validRequest = new UserRegistrationRequest();
        validRequest.setUsername("testuser");
        validRequest.setPassword("password123");
        validRequest.setRole(Roles.USER);

        savedUser = new CamsUser("testuser", "encoded_password", Roles.USER);
        savedUser.setId(1L);

        // Default mocking behavior
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
    }

    @Test
    void registerUser_WithValidRequest_ShouldRegisterUser() {
        // Arrange
        when(userRepository.findByUsername(validRequest.getUsername())).thenReturn(Optional.empty());
        when(userRepository.save(any(CamsUser.class))).thenReturn(savedUser);

        // Act
        UserDTO result = userService.registerUser(validRequest);

        // Assert
        assertNotNull(result);
        assertEquals(savedUser.getId(), result.id());
        assertEquals(savedUser.getUsername(), result.username());
        assertEquals(savedUser.getRole().name(), result.role());

        verify(userRepository).findByUsername(validRequest.getUsername());
        verify(userRepository).save(any(CamsUser.class));
        verify(passwordEncoder).encode(validRequest.getPassword());
    }

    @Test
    void registerUser_WithDuplicateUsername_ShouldThrowException() {
        // Arrange
        CamsUser existingUser = new CamsUser(validRequest.getUsername(), "existing_password", Roles.USER);
        when(userRepository.findByUsername(validRequest.getUsername())).thenReturn(Optional.of(existingUser));

        // Act & Assert
        DuplicateUsernameException exception = assertThrows(
                DuplicateUsernameException.class,
                () -> userService.registerUser(validRequest)
        );

//        assertEquals(validRequest.getUsername(), exception.());
        verify(userRepository).findByUsername(validRequest.getUsername());
        verify(userRepository, never()).save(any(CamsUser.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void registerUser_WithNullUsername_ShouldThrowException() {
        // Arrange
        validRequest.setUsername(null);

        // Mock the repository to throw an exception when save is called with null username
        when(userRepository.save(any(CamsUser.class))).thenThrow(
                new IllegalArgumentException("Username cannot be null")
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(validRequest)
        );

        assertTrue(exception.getMessage().contains("cannot be null"));
        verify(userRepository).findByUsername(null);
    }

    @Test
    void registerUser_WithEmptyUsername_ShouldThrowException() {
        // Arrange
        validRequest.setUsername("");

        // Mock the repository to throw an exception when save is called with empty username
        when(userRepository.save(any(CamsUser.class))).thenThrow(
                new IllegalArgumentException("Username cannot be empty")
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(validRequest)
        );

        assertTrue(exception.getMessage().contains("cannot be empty"));
        verify(userRepository).findByUsername("");
    }
}
