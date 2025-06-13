package com.cams.mutualfund.controller;

import com.cams.mutualfund.data.dto.ApiResponseDto;
import com.cams.mutualfund.data.dto.UserDTO;
import com.cams.mutualfund.data.request.UserRegistrationRequest;
import com.cams.mutualfund.service.UserService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/auth/users")
@Tag(name = "User Management", description = "APIs for user registration and management")
public class UserController {

    private static final Logger logger = LogManager.getLogger(UserController.class);
    
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User registered successfully",
                content = @Content(schema = @Schema(implementation = UserDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Username already exists")
    })
    public ResponseEntity<ApiResponseDto<UserDTO>> registerUser(@Valid @RequestBody UserRegistrationRequest user) {
        logger.info("Received registration request for username: {}", user.getUsername());
        UserDTO registeredUser = userService.registerUser(user);
        logger.info("User registration successful for username: {}", user.getUsername());
        return ResponseEntity.ok(ApiResponseDto.ok("User registered successfully", registeredUser));
    }
}
