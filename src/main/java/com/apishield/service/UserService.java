package com.apishield.service;

import com.apishield.dto.UserResponse;
import com.apishield.model.User;
import com.apishield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getCurrentUser(Authentication authentication) {
        String email = extractEmail(authentication);

        User user = userRepository.findByEmail(email)  // ← Changed from findByUsername to findByEmail
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        return convertToUserResponse(user);
    }

    public User getCurrentUserEntity(Authentication authentication) {
        String email = extractEmail(authentication);

        return userRepository.findByEmail(email)  // ← Changed from findByUsername to findByEmail
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    public UserResponse updateCurrency(Authentication authentication, User.Currency currency) {
        String email = extractEmail(authentication);

        User user = userRepository.findByEmail(email)  // ← Changed from findByUsername to findByEmail
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Update currency
        user.setCurrency(currency);

        // Save updated user
        User savedUser = userRepository.save(user);

        return convertToUserResponse(savedUser);
    }

    private String extractEmail(Authentication authentication) {  // ← Renamed method for clarity
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Authentication is required");
        }
        return authentication.getName();  // This returns email from UserPrincipal
    }

    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .currency(user.getCurrency())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}