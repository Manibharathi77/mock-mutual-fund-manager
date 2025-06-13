package com.cams.mutualfund.controller;

import com.cams.mutualfund.data.Roles;
import com.cams.mutualfund.data.dto.UserDTO;
import com.cams.mutualfund.data.request.UserRegistrationRequest;
import com.cams.mutualfund.exceptions.DuplicateUsernameException;
import com.cams.mutualfund.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    private UserRegistrationRequest validRequest;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set up valid registration request
        validRequest = new UserRegistrationRequest();
        validRequest.setUsername("testuser");
        validRequest.setPassword("password123");
        validRequest.setRole(Roles.USER);

        // Set up user DTO for response
        userDTO = new UserDTO(1L, "testuser", Roles.USER.name());
    }

    @Test
    void registerUser_WithValidRequest_ShouldReturnOkStatus() throws Exception {
        // Arrange
        when(userService.registerUser(any(UserRegistrationRequest.class))).thenReturn(userDTO);

        // Act & Assert
        mockMvc.perform(post("/v1/api/auth/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.id").value(userDTO.id()))
                .andExpect(jsonPath("$.data.username").value(userDTO.username()))
                .andExpect(jsonPath("$.data.role").value(userDTO.role()));

        verify(userService).registerUser(any(UserRegistrationRequest.class));
    }

    @Test
    void registerUser_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        UserRegistrationRequest invalidRequest = new UserRegistrationRequest();
        invalidRequest.setUsername(""); // Empty username
        invalidRequest.setPassword("password123");
        invalidRequest.setRole(Roles.USER);

        // Act & Assert
        mockMvc.perform(post("/v1/api/auth/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).registerUser(any(UserRegistrationRequest.class));
    }

    @Test
    void registerUser_WithDuplicateUsername_ShouldReturnConflict() throws Exception {
        // Arrange
        when(userService.registerUser(any(UserRegistrationRequest.class)))
                .thenThrow(new DuplicateUsernameException(validRequest.getUsername()));

        // Act & Assert
        mockMvc.perform(post("/v1/api/auth/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict());

        verify(userService).registerUser(any(UserRegistrationRequest.class));
    }

    @Test
    void registerUser_WithNullUsername_ShouldReturnBadRequest() throws Exception {
        // Arrange
        UserRegistrationRequest invalidRequest = new UserRegistrationRequest();
        invalidRequest.setUsername(null);
        invalidRequest.setPassword("password123");
        invalidRequest.setRole(Roles.USER);

        // Act & Assert
        mockMvc.perform(post("/v1/api/auth/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).registerUser(any(UserRegistrationRequest.class));
    }

    @Test
    void registerUser_WithNullPassword_ShouldReturnBadRequest() throws Exception {
        // Arrange
        UserRegistrationRequest invalidRequest = new UserRegistrationRequest();
        invalidRequest.setUsername("testuser");
        invalidRequest.setPassword(null);
        invalidRequest.setRole(Roles.USER);

        // Act & Assert
        mockMvc.perform(post("/v1/api/auth/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).registerUser(any(UserRegistrationRequest.class));
    }
}
