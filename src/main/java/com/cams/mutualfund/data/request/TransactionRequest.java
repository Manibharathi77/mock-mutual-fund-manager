package com.cams.mutualfund.data.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransactionRequest(
        @NotNull(message = "User ID cannot be null")
        Long userId,
        
        @NotNull(message = "Script ID cannot be null")
        Long scriptId,
        
        @NotNull(message = "Units cannot be null")
        @Positive(message = "Units must be positive")
        Double units
) {
}