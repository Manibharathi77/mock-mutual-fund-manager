package com.cams.mutualfund.data.dto;

import java.util.List;

/**
 * DTO representing a user's complete portfolio with summary information
 */
public record UserPortfolioDTO(
    Long userId,
    String username,
    List<UserHoldingDTO> holdings,
    Double totalInvestedValue,
    Double totalCurrentValue,
    Double totalProfitLoss,
    Double totalProfitLossPercentage
) {}
