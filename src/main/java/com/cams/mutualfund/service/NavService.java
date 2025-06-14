package com.cams.mutualfund.service;

import com.cams.mutualfund.data.dao.Nav;
import com.cams.mutualfund.data.dao.Script;
import com.cams.mutualfund.repository.NavRepository;
import jakarta.persistence.EntityNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class NavService implements INavService {

    private static final Logger logger = LogManager.getLogger(NavService.class);
    private final NavRepository navRepository;

    public NavService(NavRepository navRepository) {
        this.navRepository = navRepository;
    }

    /**
     * Find today's NAV for a script
     * 
     * @param script The script to find the NAV for
     * @return The NAV for today
     * @throws RuntimeException if the NAV is not found
     */
    @Override
    public Nav findTodayNav(Script script) {
        logger.debug("Fetching today's NAV for script: {}", script.getFundCode());
        return navRepository.findByScriptAndDate(script, LocalDate.now())
                .orElseThrow(() -> {
                    logger.error("NAV not found for today for script: {}", script.getFundCode());
                    return new RuntimeException("NAV not found for today");
                });
    }

    /**
     * Get the latest NAV value for a script
     * 
     * @param script The script to find the NAV for
     * @return The latest NAV value
     * @throws EntityNotFoundException if the NAV is not found
     */
    @Override
    public Double getLatestNavValue(Script script) {
        // Find the latest NAV for the script
        Optional<Nav> latestNav = navRepository.findByScriptAndDate(script, LocalDate.now());
        
        if (latestNav.isPresent()) {
            return latestNav.get().getNavValue();
        } else {
            logger.error("Latest NAV not found for script: {}", script.getFundCode());
            throw new EntityNotFoundException("Latest NAV not found for script: " + script.getFundCode());
        }
    }
}
