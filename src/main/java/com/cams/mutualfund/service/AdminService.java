package com.cams.mutualfund.service;

import com.cams.mutualfund.data.Roles;
import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.data.dao.Nav;
import com.cams.mutualfund.data.dao.Script;
import com.cams.mutualfund.data.request.CreateScriptRequest;
import com.cams.mutualfund.repository.NavRepository;
import com.cams.mutualfund.repository.ScriptRepository;
import com.cams.mutualfund.repository.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AdminService {

    private static final Logger logger = LogManager.getLogger(AdminService.class);

    private final ScriptRepository scriptRepository;
    private final UserRepository userRepository;
    private final NavRepository navRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(ScriptRepository scriptRepository, UserRepository userRepository,
                        NavRepository navRepository, PasswordEncoder passwordEncoder) {
        this.scriptRepository = scriptRepository;
        this.userRepository = userRepository;
        this.navRepository = navRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Script createScript(CreateScriptRequest request) {
        logger.info("Creating new script with fund code: {}", request.fundCode());
        
        if (scriptRepository.findByFundCode(request.fundCode()).isPresent()) {
            logger.warn("Script creation failed: Fund code {} already exists", request.fundCode());
            throw new IllegalArgumentException("Script with fund code already exists");
        }

        Script script = new Script(
                request.fundCode(),
                request.name(),
                request.category(),
                request.amc()
        );

        Script savedScript = scriptRepository.save(script);
        logger.info("Script created successfully with ID: {}", savedScript.getId());
        return savedScript;
    }

    public void addNavForToday(String fundCode, Double navValue) {
        logger.info("Adding NAV for fund code: {}, value: {}", fundCode, navValue);
        
        Script script = scriptRepository.findByFundCode(fundCode)
                .orElseThrow(() -> {
                    logger.error("NAV addition failed: Script not found with fund code: {}", fundCode);
                    return new IllegalArgumentException("Script not found");
                });

        LocalDate today = LocalDate.now();
        logger.debug("Checking if NAV already exists for date: {}", today);

        boolean exists = navRepository.findByScriptAndDate(script, today).isPresent();
        if (exists) {
            logger.warn("NAV addition failed: NAV already exists for today for fund code: {}", fundCode);
            throw new IllegalStateException("NAV for today already exists");
        }

        Nav nav = new Nav(today, navValue, script);
        navRepository.save(nav);
        logger.info("NAV added successfully for fund code: {} on date: {}", fundCode, today);
    }

    public List<CamsUser> getAllUsers() {
        logger.info("Retrieving all users");
        List<CamsUser> users = userRepository.findAll();
        logger.info("Retrieved {} users", users.size());
        return users;
    }

    public void createUser(String username, String password, String roleName) {
        logger.info("Creating new user with username: {}, role: {}", username, roleName);
        
        if (userRepository.findByUsername(username).isPresent()) {
            logger.warn("User creation failed: Username {} already exists", username);
            throw new IllegalArgumentException("Username already exists");
        }

        Roles role = Roles.valueOf(roleName.toUpperCase());
        String encodedPassword = passwordEncoder.encode(password);

        CamsUser savedUser = userRepository.save(new CamsUser(username, encodedPassword, role));
        logger.info("User created successfully with ID: {}", savedUser.getId());
    }

    public void deleteUser(Long userId) {
        logger.info("Deleting user with ID: {}", userId);
        
        if (!userRepository.existsById(userId)) {
            logger.warn("User deletion failed: User not found with ID: {}", userId);
            throw new IllegalArgumentException("CamsUser not found");
        }
        
        userRepository.deleteById(userId);
        logger.info("User deleted successfully with ID: {}", userId);
    }
}
