package com.microecommerce.user_service.controller;

import com.microecommerce.user_service.dto.LoginRequestDTO;
import com.microecommerce.user_service.dto.LoginResponseDTO;
import com.microecommerce.user_service.dto.UserDTO;
import com.microecommerce.user_service.model.User;
import com.microecommerce.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public User register(@RequestBody UserDTO userDTO) {
        return userService.CreateUser(userDTO);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        String token = userService.login(loginRequestDTO);
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }
}
