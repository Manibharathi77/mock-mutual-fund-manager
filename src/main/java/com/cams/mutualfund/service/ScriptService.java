package com.cams.mutualfund.service;

import com.cams.mutualfund.data.dao.Script;
import com.cams.mutualfund.repository.ScriptRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class ScriptService implements IScriptService {

    private static final Logger logger = LogManager.getLogger(ScriptService.class);
    private final ScriptRepository scriptRepository;

    public ScriptService(ScriptRepository scriptRepository) {
        this.scriptRepository = scriptRepository;
    }

    /**
     * Find a script by its ID
     * 
     * @param scriptId The ID of the script to find
     * @return The found script
     * @throws RuntimeException if the script is not found
     */
    @Override
    public Script findScript(Long scriptId) {
        return scriptRepository.findById(scriptId)
                .orElseThrow(() -> {
                    logger.error("Script not found with ID: {}", scriptId);
                    return new RuntimeException("Script not found");
                });
    }
}
