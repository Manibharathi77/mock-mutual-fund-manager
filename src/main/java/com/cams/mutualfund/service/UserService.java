package com.cams.mutualfund.service;

import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.repository.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class UserService implements IUserService {

    private static final Logger logger = LogManager.getLogger(UserService.class);
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Find a user by their ID
     * 
     * @param userId The ID of the user to find
     * @return The found user
     * @throws RuntimeException if the user is not found
     */
    @Override
    public CamsUser findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("User not found with ID: {}", userId);
                    return new RuntimeException("CamsUser not found");
                });
    }
}
