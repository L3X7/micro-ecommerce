package com.microecommerce.product_service.service;

import com.microecommerce.product_service.dto.ProductDTO;
import com.microecommerce.product_service.model.Product;
import com.microecommerce.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product CreateProduct(ProductDTO productDTO) {
        Product newProduct = new Product(productDTO);
        return productRepository.save(newProduct);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

}
