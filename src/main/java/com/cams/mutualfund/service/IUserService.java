package com.cams.mutualfund.service;

import com.cams.mutualfund.data.dao.CamsUser;

/**
 * Interface for user-related operations
 */
public interface IUserService {
    
    /**
     * Find a user by their ID
     * 
     * @param userId The ID of the user to find
     * @return The found user
     * @throws RuntimeException if the user is not found
     */
    CamsUser findUser(Long userId);
}
