package com.cams.mutualfund.controller;

import com.cams.mutualfund.data.Roles;
import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.data.dao.Script;
import com.cams.mutualfund.data.dto.UserDTO;
import com.cams.mutualfund.data.request.CreateScriptRequest;
import com.cams.mutualfund.data.request.NavRequest;
import com.cams.mutualfund.data.request.UserRegistrationRequest;
import com.cams.mutualfund.exceptions.DuplicateUsernameException;
import com.cams.mutualfund.handler.GlobalExceptionHandler;
import com.cams.mutualfund.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private Script testScript;
    private CreateScriptRequest createScriptRequest;
    private CamsUser testUser;
    private UserRegistrationRequest createUserRequest;
    private NavRequest navRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        // Set up test script
        testScript = new Script("TEST123", "Test Fund", "Equity", "Test AMC");
        testScript.setId(1L);

        createScriptRequest = new CreateScriptRequest(
                "TEST123",
                "Test Fund",
                "Equity",
                "Test AMC"
        );

        // Set up test user
        testUser = new CamsUser("testuser", "password", Roles.USER);
        testUser.setId(1L);

        createUserRequest = new UserRegistrationRequest(
                "testuser",
                "password",
                Roles.USER.name()
        );

        // Set up NAV request
        navRequest = new NavRequest();
        navRequest.setNav(25.5);
    }

    @Test
    void createScript_WithValidRequest_ShouldReturnCreatedStatus() throws Exception {
        // Arrange
        when(adminService.createScript(any(CreateScriptRequest.class))).thenReturn(testScript);

        // Act & Assert
        mockMvc.perform(post("/v1/api/admin/scripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createScriptRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Script created successfully"))
                .andExpect(jsonPath("$.data.id").value(testScript.getId()))
                .andExpect(jsonPath("$.data.fundCode").value(testScript.getFundCode()))
                .andExpect(jsonPath("$.data.name").value(testScript.getName()));

        verify(adminService).createScript(any(CreateScriptRequest.class));
    }

    @Test
    void createScript_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        CreateScriptRequest invalidRequest = new CreateScriptRequest(
                "", // Empty fund code
                "Test Fund",
                "Equity",
                "Test AMC"
        );

        // Act & Assert
        mockMvc.perform(post("/v1/api/admin/scripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(adminService, never()).createScript(any(CreateScriptRequest.class));
    }

    @Test
    void createScript_WhenServiceThrowsException_ShouldReturnBadRequest() throws Exception {
        // Arrange
        when(adminService.createScript(any(CreateScriptRequest.class)))
                .thenThrow(new IllegalArgumentException("Script already exists"));

        // Act & Assert
        mockMvc.perform(post("/v1/api/admin/scripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createScriptRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Script already exists"));

        verify(adminService).createScript(any(CreateScriptRequest.class));
    }

    @Test
    void addTodayNav_WithValidRequest_ShouldReturnCreatedStatus() throws Exception {
        // Arrange
        doNothing().when(adminService).addNavForToday(anyString(), anyDouble());

        // Act & Assert
        mockMvc.perform(post("/v1/api/admin/scripts/{fundCode}/nav", "TEST123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(navRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("NAV added successfully"));

        verify(adminService).addNavForToday(anyString(), anyDouble());
    }

    @Test
    void addTodayNav_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        NavRequest invalidRequest = new NavRequest();
        // Nav value is null

        // Act & Assert
        mockMvc.perform(post("/v1/api/admin/scripts/{fundCode}/nav", "TEST123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(adminService, never()).addNavForToday(anyString(), anyDouble());
    }

    @Test
    void addTodayNav_WhenScriptNotFound_ShouldReturnBadRequest() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("Script not found with code: TEST999"))
                .when(adminService).addNavForToday(eq("TEST999"), anyDouble());

        // Act & Assert
        mockMvc.perform(post("/v1/api/admin/scripts/{fundCode}/nav", "TEST999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(navRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Script not found with code: TEST999"));

        verify(adminService).addNavForToday(eq("TEST999"), anyDouble());
    }

    @Test
    void listAllUsers_ShouldReturnAllUsers() throws Exception {
        // Arrange
        List<CamsUser> users = Arrays.asList(
                testUser,
                new CamsUser("admin", "adminpass", Roles.ADMIN)
        );
        when(adminService.getAllUsers()).thenReturn(users);

        // Act & Assert
        mockMvc.perform(get("/v1/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Users retrieved successfully"))
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(testUser.getId()))
                .andExpect(jsonPath("$.data[0].username").value(testUser.getUsername()));

        verify(adminService).getAllUsers();
    }

    @Test
    void listAllUsers_WhenServiceThrowsException_ShouldReturnServerError() throws Exception {
        // Arrange
        when(adminService.getAllUsers()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/v1/api/admin/users"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Unexpected error: Database error"));

        verify(adminService).getAllUsers();
    }

    @Test
    void createUser_WithValidRequest_ShouldReturnCreatedStatus() throws Exception {
        // Arrange
        UserDTO userDTO = new UserDTO(1L, "testuser", Roles.USER.name());
        when(adminService.registerUser(any(UserRegistrationRequest.class))).thenReturn(userDTO);

        // Act & Assert
        mockMvc.perform(post("/v1/api/admin/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.id").value(userDTO.id()))
                .andExpect(jsonPath("$.data.username").value(userDTO.username()));

        verify(adminService).registerUser(any(UserRegistrationRequest.class));
    }

    @Test
    void createUser_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        UserRegistrationRequest invalidRequest = new UserRegistrationRequest(
                "", // Empty username
                "password",
                Roles.USER.name()
        );

        // Act & Assert
        mockMvc.perform(post("/v1/api/admin/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(adminService, never()).registerUser(any(UserRegistrationRequest.class));
    }

    @Test
    void createUser_WhenUsernameAlreadyExists_ShouldReturnBadRequest() throws Exception {
        // Arrange
        when(adminService.registerUser(any(UserRegistrationRequest.class)))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        // Act & Assert
        mockMvc.perform(post("/v1/api/admin/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Username already exists"));

        verify(adminService).registerUser(any(UserRegistrationRequest.class));
    }

    @Test
    void createUser_WhenDuplicateUsernameExceptionThrown_ShouldReturnBadRequest() throws Exception {
        // Arrange
        String username = "testuser";
        when(adminService.registerUser(any(UserRegistrationRequest.class)))
                .thenThrow(new DuplicateUsernameException(username));

        // Act & Assert
        mockMvc.perform(post("/v1/api/admin/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Username 'testuser' already exists"));

        verify(adminService).registerUser(any(UserRegistrationRequest.class));
    }

    @Test
    void deleteUser_WithValidId_ShouldReturnOkStatus() throws Exception {
        // Arrange
        doNothing().when(adminService).deleteUser(anyLong());

        // Act & Assert
        mockMvc.perform(delete("/v1/api/admin/users/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        verify(adminService).deleteUser(anyLong());
    }

    @Test
    void deleteUser_WhenUserNotFound_ShouldReturnBadRequest() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("User not found with ID: 999"))
                .when(adminService).deleteUser(anyLong());

        // Act & Assert
        mockMvc.perform(delete("/v1/api/admin/users/{userId}", 999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("User not found with ID: 999"));

        verify(adminService).deleteUser(anyLong());
    }
}
