package com.cams.mutualfund.service;

import com.cams.mutualfund.data.dao.Script;

/**
 * Interface for script-related operations
 */
public interface IScriptService {
    
    /**
     * Find a script by its ID
     * 
     * @param scriptId The ID of the script to find
     * @return The found script
     * @throws RuntimeException if the script is not found
     */
    Script findScript(Long scriptId);
}
