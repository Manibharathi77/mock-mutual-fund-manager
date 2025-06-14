package com.cams.mutualfund.service;

import com.cams.mutualfund.data.dao.Nav;
import com.cams.mutualfund.data.dao.Script;
import jakarta.persistence.EntityNotFoundException;

/**
 * Interface for NAV-related operations
 */
public interface INavService {
    
    /**
     * Find today's NAV for a script
     * 
     * @param script The script to find the NAV for
     * @return The NAV for today
     * @throws RuntimeException if the NAV is not found
     */
    Nav findTodayNav(Script script);
    
    /**
     * Get the latest NAV value for a script
     * 
     * @param script The script to find the NAV for
     * @return The latest NAV value
     * @throws EntityNotFoundException if the NAV is not found
     */
    Double getLatestNavValue(Script script);
}
