package com.cams.mutualfund.controller;

import com.cams.mutualfund.data.dto.ApiResponseDto;
import com.cams.mutualfund.data.dto.UserPortfolioDTO;
import com.cams.mutualfund.data.request.TransactionRequest;
import com.cams.mutualfund.facade.IUserTransactionFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/transactions")
@Tag(name = "User Transactions", description = "APIs for user transaction operations")
public class UserTransactionController {

    private static final Logger logger = LogManager.getLogger(UserTransactionController.class);
    
    private final IUserTransactionFacade userTransactionFacade;

    public UserTransactionController(IUserTransactionFacade userTransactionFacade) {
        this.userTransactionFacade = userTransactionFacade;
    }

    @GetMapping("/portfolio/{userId}")
    @Operation(summary = "Get user portfolio", description = "Retrieves a user's complete portfolio with profit/loss information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Portfolio retrieved successfully", 
                content = @Content(schema = @Schema(implementation = UserPortfolioDTO.class))),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponseDto<?>> getUserPortfolio(@PathVariable Long userId) {
        logger.info("Fetching portfolio for user ID: {}", userId);
        UserPortfolioDTO portfolio = userTransactionFacade.getUserPortfolio(userId);
        logger.info("Portfolio fetched successfully for user ID: {}", userId);
        return ResponseEntity.ok(ApiResponseDto.ok("Portfolio retrieved successfully", portfolio));
    }

    @PostMapping("/buy")
    @Operation(summary = "Buy mutual fund units", description = "Purchases specified units of a mutual fund")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Units purchased successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "User or script not found")
    })
    public ResponseEntity<ApiResponseDto<?>> buyUnits(@Valid @RequestBody TransactionRequest request) {
        logger.info("Processing buy request - User ID: {}, Script ID: {}, Units: {}", 
                request.userId(), request.scriptId(), request.units());
        userTransactionFacade.buyUnits(request.userId(), request.scriptId(), request.units());
        logger.info("Units purchased successfully for User ID: {}, Script ID: {}", 
                request.userId(), request.scriptId());
        return ResponseEntity.ok(ApiResponseDto.success("Units purchased successfully"));
    }

    @PostMapping("/redeem")
    @Operation(summary = "Redeem mutual fund units", description = "Redeems specified units of a mutual fund")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Units redeemed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "User or script not found")
    })
    public ResponseEntity<ApiResponseDto<?>> redeemUnits(@Valid @RequestBody TransactionRequest request) {
        logger.info("Processing redemption request - User ID: {}, Script ID: {}, Units: {}", 
                request.userId(), request.scriptId(), request.units());
        userTransactionFacade.redeemUnits(request.userId(), request.scriptId(), request.units());
        logger.info("Units redeemed successfully for User ID: {}, Script ID: {}", 
                request.userId(), request.scriptId());
        return ResponseEntity.ok(ApiResponseDto.success("Units redeemed successfully"));
    }
}
