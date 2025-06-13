package com.cams.mutualfund.service;

import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.data.dto.UserDTO;
import com.cams.mutualfund.data.request.UserRegistrationRequest;
import com.cams.mutualfund.exceptions.DuplicateUsernameException;
import com.cams.mutualfund.repository.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service class for user operations.
 */
@Service
public class UserService {

    private static final Logger logger = LogManager.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user.
     *
     * @param user the user registration request
     * @return the registered user DTO
     * @throws DuplicateUsernameException if the username already exists
     */
    public UserDTO registerUser(UserRegistrationRequest user) {
        logger.info("Processing registration request for username: {}", user.getUsername());
        
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            logger.warn("Registration failed: Username '{}' already exists", user.getUsername());
            throw new DuplicateUsernameException(user.getUsername());
        }
        
        logger.debug("Username available, proceeding with registration");
        CamsUser savedCamsUser = userRepository.save(new CamsUser(user.getUsername(), 
                passwordEncoder.encode(user.getPassword()), 
                user.getRole()));
        
        logger.info("User successfully registered with ID: {}", savedCamsUser.getId());
        return new UserDTO(savedCamsUser.getId(), savedCamsUser.getUsername(), savedCamsUser.getRole().name());
    }

    private boolean checkIfNameAvailable(String username) {
        logger.debug("Checking if username is available: {}", username);
        return userRepository.findByUsername(username).isPresent();
    }

}
