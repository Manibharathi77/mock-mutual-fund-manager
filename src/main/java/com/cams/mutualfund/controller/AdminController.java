package com.cams.mutualfund.controller;

import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.data.dao.Script;
import com.cams.mutualfund.data.dto.ApiResponseDto;
import com.cams.mutualfund.data.request.CreateScriptRequest;
import com.cams.mutualfund.data.request.CreateUserRequest;
import com.cams.mutualfund.data.request.NavRequest;
import com.cams.mutualfund.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/admin")
@Tag(name = "Admin Operations", description = "APIs for administrative operations")
//@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LogManager.getLogger(AdminController.class);

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/scripts")
    @Operation(summary = "Create a new mutual fund script", description = "Creates a new mutual fund script with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Script created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<ApiResponseDto<Script>> createScript(@Valid @RequestBody CreateScriptRequest request) {
        logger.info("Creating new script with code: {}", request.fundCode());
        Script created = adminService.createScript(request);
        logger.info("Script created successfully with ID: {}", created.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDto.created("Script created successfully", created));
    }

    @PostMapping("/scripts/{fundCode}/nav")
    @Operation(summary = "Add today's NAV for a fund", description = "Adds today's NAV value for the specified fund")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "NAV added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or fund code")
    })
    public ResponseEntity<ApiResponseDto<?>> addTodayNav(
            @PathVariable @Valid String fundCode,
            @Valid @RequestBody NavRequest request) {
        logger.info("Adding NAV for fund code: {}, value: {}", fundCode, request.getNav());
        adminService.addNavForToday(fundCode, request.getNav());
        logger.info("NAV added successfully for fund: {}", fundCode);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDto.created("NAV added successfully", null));
    }

    @GetMapping("/users")
    @Operation(summary = "List all users", description = "Retrieves a list of all registered users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CamsUser.class)))
    })
    public ResponseEntity<ApiResponseDto<List<CamsUser>>> listAllUsers() {
        logger.info("Retrieving all users");
        List<CamsUser> users = adminService.getAllUsers();
        logger.info("Retrieved {} users", users.size());
        return ResponseEntity.ok(ApiResponseDto.ok("Users retrieved successfully", users));
    }

    @PostMapping("/users")
    @Operation(summary = "Create a new user", description = "Creates a new user with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<ApiResponseDto<?>> createUser(@Valid @RequestBody CreateUserRequest request) {
        logger.info("Creating new user with username: {}, role: {}", request.username(), request.role());
        adminService.createUser(request.username(), request.password(), request.role());
        logger.info("User created successfully: {}", request.username());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDto.created("User created successfully", null));
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete a user", description = "Deletes the user with the specified ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID")
    })
    public ResponseEntity<ApiResponseDto<?>> deleteUser(@PathVariable Long userId) {
        logger.info("Deleting user with ID: {}", userId);
        adminService.deleteUser(userId);
        logger.info("User deleted successfully: {}", userId);
        return ResponseEntity.ok(ApiResponseDto.success("User deleted successfully"));
    }
}