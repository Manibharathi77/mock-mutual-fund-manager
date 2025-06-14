package com.cams.mutualfund.service;

import com.cams.mutualfund.data.Roles;
import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private CamsUser testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up test user
        testUser = new CamsUser("testuser", "password", Roles.USER);
        testUser.setId(1L);
    }

    @Test
    void loadUserByUsername_WithValidUsername_ShouldReturnUserDetails() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");
        
        // Assert
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("password", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertEquals(1, userDetails.getAuthorities().size());
    }
    
    @Test
    void loadUserByUsername_WithAdminRole_ShouldReturnAdminAuthority() {
        // Arrange
        CamsUser adminUser = new CamsUser("admin", "adminpass", Roles.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        
        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("admin");
        
        // Assert
        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_WithInvalidUsername_ShouldThrowException() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        
        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> customUserDetailsService.loadUserByUsername("nonexistentuser")
        );
        
        assertEquals("CamsUser not found", exception.getMessage());
    }
}
