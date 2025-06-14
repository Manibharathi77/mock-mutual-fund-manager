package com.cams.mutualfund.controller;

import com.cams.mutualfund.data.request.TransactionRequest;
import com.cams.mutualfund.exceptions.GlobalExceptionHandler;
import com.cams.mutualfund.service.UserTransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserTransactionControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private UserTransactionService userTransactionService;
    
    @InjectMocks
    private UserTransactionController userTransactionController;

    private TransactionRequest validRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(userTransactionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        // Set up valid transaction request
        validRequest = new TransactionRequest(1L, 1L, 100.0);
    }

    @Test
    void buyUnits_WithValidRequest_ShouldReturnOkStatus() throws Exception {
        // Arrange
        doNothing().when(userTransactionService).buyUnits(anyLong(), anyLong(), anyDouble());

        // Act & Assert
        mockMvc.perform(post("/v1/api/transactions/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Units purchased successfully"));

        verify(userTransactionService).buyUnits(
                validRequest.userId(),
                validRequest.scriptId(),
                validRequest.units()
        );
    }

    @Test
    void buyUnits_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        TransactionRequest invalidRequest = new TransactionRequest(null, 1L, 100.0);

        // Act & Assert
        mockMvc.perform(post("/v1/api/transactions/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userTransactionService, never()).buyUnits(anyLong(), anyLong(), anyDouble());
    }

    @Test
    void buyUnits_WithNegativeUnits_ShouldReturnBadRequest() throws Exception {
        // Arrange
        TransactionRequest invalidRequest = new TransactionRequest(1L, 1L, -50.0);

        // Act & Assert
        mockMvc.perform(post("/v1/api/transactions/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userTransactionService, never()).buyUnits(anyLong(), anyLong(), anyDouble());
    }

    @Test
    void buyUnits_WithUserNotFound_ShouldReturnServerError() throws Exception {
        // Arrange
        doThrow(new RuntimeException("CamsUser not found")).when(userTransactionService)
                .buyUnits(anyLong(), anyLong(), anyDouble());

        // Act & Assert
        mockMvc.perform(post("/v1/api/transactions/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Unexpected error: CamsUser not found"));

        verify(userTransactionService).buyUnits(
                validRequest.userId(),
                validRequest.scriptId(),
                validRequest.units()
        );
    }

    @Test
    void redeemUnits_WithValidRequest_ShouldReturnOkStatus() throws Exception {
        // Arrange
        doNothing().when(userTransactionService).redeemUnits(anyLong(), anyLong(), anyDouble());

        // Act & Assert
        mockMvc.perform(post("/v1/api/transactions/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Units redeemed successfully"));

        verify(userTransactionService).redeemUnits(
                validRequest.userId(),
                validRequest.scriptId(),
                validRequest.units()
        );
    }

    @Test
    void redeemUnits_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        TransactionRequest invalidRequest = new TransactionRequest(1L, null, 100.0);

        // Act & Assert
        mockMvc.perform(post("/v1/api/transactions/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userTransactionService, never()).redeemUnits(anyLong(), anyLong(), anyDouble());
    }

    @Test
    void redeemUnits_WithInsufficientUnits_ShouldReturnBadRequest() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("Insufficient units to redeem. Available: 10.0")).when(userTransactionService)
                .redeemUnits(anyLong(), anyLong(), anyDouble());

        // Act & Assert
        mockMvc.perform(post("/v1/api/transactions/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Insufficient units to redeem. Available: 10.0"));

        verify(userTransactionService).redeemUnits(
                validRequest.userId(),
                validRequest.scriptId(),
                validRequest.units()
        );
    }

    @Test
    void redeemUnits_WithNegativeUnits_ShouldReturnBadRequest() throws Exception {
        // Arrange
        TransactionRequest invalidRequest = new TransactionRequest(1L, 1L, -50.0);

        // Act & Assert
        mockMvc.perform(post("/v1/api/transactions/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userTransactionService, never()).redeemUnits(anyLong(), anyLong(), anyDouble());
    }
}
