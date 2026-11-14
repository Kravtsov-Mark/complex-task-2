package com.example.productcatalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductRepository productRepository;

    @Test
    public void shouldReturnAllProducts() throws Exception {
        when(productRepository.findAll()).thenReturn(Arrays.asList(new Product()));
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnProductByIdWhenFound() throws Exception {
        Product product = new Product();
        product.setId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnNotFoundWhenProductNotFound() throws Exception {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/products/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldCreateProduct() throws Exception {
        Product product = new Product();
        product.setId(1L);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        String json = "{\"name\":\"Test\",\"price\":100.0}";
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }
}
