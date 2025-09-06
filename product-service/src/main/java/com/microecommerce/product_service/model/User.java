package com.microecommerce.product_service.model;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
}
