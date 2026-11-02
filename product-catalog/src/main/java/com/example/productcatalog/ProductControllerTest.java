package com.example.productcatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnAllProducts() throws Exception {

        Product product1 = new Product(1L, "Laptop", 1200.50);
        Product product2 = new Product(2L, "Smartphone", 800.00);
        when(productService.getAllProducts()).thenReturn(List.of(product1, product2));

        // Act & Assert
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$[1].name").value("Smartphone"));
    }

    @Test
    void shouldReturnProductByIdWhenFound() throws Exception {
        // Arrange
        Product product = new Product(1L, "Laptop", 1200.50);
        when(productService.getProductById(1L)).thenReturn(Optional.of(product));

        // Act & Assert
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void shouldReturnNotFoundWhenProductByIdNotFound() throws Exception {
        // Arrange
        when(productService.getProductById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateNewProduct() throws Exception {
        // Arrange
        Product newProduct = new Product(null, "Keyboard", 75.00);
        Product savedProduct = new Product(3L, "Keyboard", 75.00);
        when(productService.createProduct(any(Product.class))).thenReturn(savedProduct);

        // Act & Assert
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.name").value("Keyboard"));
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        // Arrange
        Product updatedProduct = new Product(1L, "Gaming Laptop", 1500.00);
        when(productService.updateProduct(eq(1L), any(Product.class))).thenReturn(Optional.of(updatedProduct));

        // Act & Assert
        mockMvc.perform(put("/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gaming Laptop"))
                .andExpect(jsonPath("$.price").value(1500.00));
    }
}
