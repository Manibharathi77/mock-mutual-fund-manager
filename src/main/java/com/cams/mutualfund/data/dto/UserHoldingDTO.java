package com.cams.mutualfund.data.dto;

/**
 * DTO representing a user's holding of a mutual fund with current value and profit/loss information
 */
public record UserHoldingDTO(
    Long holdingId,
    Long scriptId,
    String fundCode,
    String fundName,
    String category,
    String amc,
    Double units,
    Double currentNavValue,
    Double currentTotalValue,
    Double investedValue,
    Double profitLoss,
    Double profitLossPercentage
) {}
