package com.cams.mutualfund.controller;

import com.cams.mutualfund.data.Roles;
import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.data.dao.Script;
import com.cams.mutualfund.data.request.CreateScriptRequest;
import com.cams.mutualfund.data.request.CreateUserRequest;
import com.cams.mutualfund.data.request.NavRequest;
import com.cams.mutualfund.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private AdminService adminService;

    private Script testScript;
    private CreateScriptRequest createScriptRequest;
    private CamsUser testUser;
    private CreateUserRequest createUserRequest;
    private NavRequest navRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

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

        createUserRequest = new CreateUserRequest(
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
//        when(adminService.createScript(any(CreateScriptRequest.class))).thenReturn(testScript);

        // Act & Assert
        mockMvc.perform(post("/v1/api/admin/scripts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createScriptRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Script created successfully"))
                .andExpect(jsonPath("$.data.id").value(testScript.getId()))
                .andExpect(jsonPath("$.data.fundCode").value(testScript.getFundCode()))
                .andExpect(jsonPath("$.data.name").value(testScript.getName()));

//        verify(adminService).createScript(any(CreateScriptRequest.class));
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

//        verify(adminService, never()).createScript(any(CreateScriptRequest.class));
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
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("NAV added successfully"));

        verify(adminService).addNavForToday("TEST123", navRequest.getNav());
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
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Users retrieved successfully"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(testUser.getId()))
                .andExpect(jsonPath("$.data[0].username").value(testUser.getUsername()));

        verify(adminService).getAllUsers();
    }

    @Test
    void createUser_WithValidRequest_ShouldReturnCreatedStatus() throws Exception {
        // Arrange
//        doNothing().when(adminService).createUser(anyString(), anyString(), any(Roles.class));

        // Act & Assert
        mockMvc.perform(post("/v1/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("User created successfully"));

        verify(adminService).createUser(
                createUserRequest.username(),
                createUserRequest.password(),
                createUserRequest.role()
        );
    }

    @Test
    void createUser_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        CreateUserRequest invalidRequest = new CreateUserRequest(
                "", // Empty username
                "password",
                Roles.USER.name()
        );

        // Act & Assert
        mockMvc.perform(post("/v1/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

//        verify(adminService, never()).createUser(anyString(), anyString(), any(Roles.class));
    }

    @Test
    void deleteUser_WithValidId_ShouldReturnOkStatus() throws Exception {
        // Arrange
        doNothing().when(adminService).deleteUser(anyLong());

        // Act & Assert
        mockMvc.perform(delete("/v1/api/admin/users/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        verify(adminService).deleteUser(1L);
    }

    @Test
    void deleteUser_WithInvalidId_ShouldReturnBadRequest() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("User not found")).when(adminService).deleteUser(anyLong());

        // Act & Assert
        mockMvc.perform(delete("/v1/api/admin/users/{userId}", 999L))
                .andExpect(status().isBadRequest());

        verify(adminService).deleteUser(999L);
    }
}
