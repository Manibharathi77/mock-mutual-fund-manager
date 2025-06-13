package com.cams.mutualfund.data.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class NavRequest {
    @NotNull(message = "NAV value is required")
    @Positive(message = "NAV value must be positive")
    private Double nav;  // today's NAV value

    // Getters and setters

    public Double getNav() {
        return nav;
    }

    public void setNav(Double nav) {
        this.nav = nav;
    }
}