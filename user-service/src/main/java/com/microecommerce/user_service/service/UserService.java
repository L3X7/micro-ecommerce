package com.microecommerce.user_service.service;

import com.microecommerce.user_service.dto.LoginRequestDTO;
import com.microecommerce.user_service.dto.AuthResponseDTO;
import com.microecommerce.user_service.dto.RefreshTokenRequestDTO;
import com.microecommerce.user_service.dto.UserDTO;
import com.microecommerce.user_service.model.User;
import com.microecommerce.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public User CreateUser(UserDTO userDTO) {
        User newUser = new User(userDTO);
        newUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        return userRepository.save(newUser);
    }

    public AuthResponseDTO login(LoginRequestDTO loginRequestDTO) {
        var user = userRepository.findByUsername(loginRequestDTO.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
            String token = jwtService.generateToken(user.getUsername(), null);
            String refreshToken = jwtService.generateRefreshToken(user.getUsername(), null);
            return new AuthResponseDTO(token, refreshToken);
        }
        throw new RuntimeException("Invalid password");
    }

    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO refreshTokenRequestDTO) {
        String newRefreshToken = jwtService.generateNewRefreshToken(refreshTokenRequestDTO.getRefreshToken());
        String username = jwtService.extractUsername(newRefreshToken);
        String newToken = jwtService.generateToken(username, null);
        return new AuthResponseDTO(newToken, newRefreshToken);
    }
}
