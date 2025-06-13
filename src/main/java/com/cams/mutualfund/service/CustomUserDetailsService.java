package com.cams.mutualfund.service;

import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // scope to add more custom authentication logic if required.

        CamsUser camsUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("CamsUser not found"));

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + camsUser.getRole());

        return new org.springframework.security.core.userdetails.User(
                camsUser.getUsername(),
                camsUser.getPassword(),
                List.of(authority)
        );
    }
}
