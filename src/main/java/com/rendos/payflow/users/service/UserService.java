package com.rendos.payflow.users.service;

import com.rendos.payflow.common.exception.DuplicateResourceException;
import com.rendos.payflow.common.exception.ResourceNotFoundException;
import com.rendos.payflow.users.dto.CreateUserRequest;
import com.rendos.payflow.users.dto.UserResponse;
import com.rendos.payflow.users.entity.User;
import com.rendos.payflow.users.entity.UserRole;
import com.rendos.payflow.users.entity.UserStatus;
import com.rendos.payflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User already exists with email: " + request.getEmail());
        }

        // Note: Password hash will plug into BCrypt / Spring Security in Phase 3
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash("{noop}" + request.getPassword())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);
        log.info("Created new user: id={}, email={}", saved.getId(), saved.getEmail());
        return UserResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return UserResponse.fromEntity(user);
    }
}

