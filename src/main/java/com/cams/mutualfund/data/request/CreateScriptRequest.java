package com.cams.mutualfund.data.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateScriptRequest(
        @NotBlank(message = "Fund code is required")
        @Pattern(regexp = "^[A-Z0-9-]+$", message = "Fund code must contain only uppercase letters, numbers, and hyphens")
        @Size(max = 20, message = "Fund code cannot exceed 20 characters")
        String fundCode,

        @NotBlank(message = "Fund name is required")
        @Size(min = 3, max = 100, message = "Fund name must be between 3 and 100 characters")
        String name,

        @NotBlank(message = "Category is required")
        @Size(max = 50, message = "Category cannot exceed 50 characters")
        String category,

        @NotBlank(message = "AMC is required")
        @Size(max = 100, message = "AMC name cannot exceed 100 characters")
        String amc
) {
    // All the getters, equals(), hashCode(), and toString() are automatically generated
}