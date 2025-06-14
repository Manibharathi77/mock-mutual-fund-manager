package com.cams.mutualfund.service;

import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private CamsUser testUser;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set up test user
        testUser = new CamsUser();
        testUser.setId(userId);
        testUser.setUsername("testuser");
    }

    @Test
    void findUser_WhenUserExists_ShouldReturnUser() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        CamsUser result = userService.findUser(userId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("testuser", result.getUsername());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void findUser_WhenUserDoesNotExist_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.findUser(userId);
        });

        assertEquals("CamsUser not found", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
    }
}
