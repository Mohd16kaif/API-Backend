package com.apishield.dto;

import com.apishield.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String username;
    private String email;
    private User.Currency currency;
    private User.Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}