package com.bookfair.backend.security;

import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.bookfair.backend.model.User;
import com.bookfair.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Cacheable(value = "userDetails", key = "#username")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository
        .findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        if(!Boolean.TRUE.equals(user.getActive())) {
            throw new DisabledException("User account is deactivated");
        }

        return new CustomUserPrincipal(user);
    }

    @Cacheable(value = "userDetailsById", key = "#id")
    public UserDetails loadUserById(UUID id) {
        User user = userRepository
        .findById(id)
        .orElseThrow(() -> new UsernameNotFoundException("User not found with userId: " + id));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new DisabledException("User account is deactivated");
        }

        return new CustomUserPrincipal(user);
    }



    @Caching(evict = {
        @CacheEvict(value = "userDetails", key = "#username"),
        @CacheEvict(value = "userDetailsById", key = "#userId")
    })
    public void evictUserDetails(UUID userId, String username) {
        
        /**
         * Evicts cached user details after account changes such as:
         * - deactivation
         * - reactivation
         * - role changes
         * - password changes
         * - username changes
        */
       
    }

}
