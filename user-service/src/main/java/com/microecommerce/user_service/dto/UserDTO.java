package com.microecommerce.user_service.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private String email;
    private String phone;
}
