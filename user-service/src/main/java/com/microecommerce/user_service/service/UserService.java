package com.microecommerce.user_service.service;

import com.microecommerce.user_service.dto.LoginRequestDTO;
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

    public String login(LoginRequestDTO loginRequestDTO) {
        var user = userRepository.findByUsername(loginRequestDTO.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
            return jwtService.generateToken(user);
        }
        throw new RuntimeException("Invalid password");
    }
}
