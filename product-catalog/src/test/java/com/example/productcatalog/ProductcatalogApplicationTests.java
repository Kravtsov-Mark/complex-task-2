package com.example.productcatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	// 1. GET всі
	@Test
	void shouldReturnAllProducts() throws Exception {
		mockMvc.perform(get("/products"))
				.andExpect(status().isOk());
	}

	// 2. GET за ID (Успіх)
	@Test
	void shouldReturnProductByIdWhenFound() throws Exception {
		mockMvc.perform(get("/products/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	// 3. GET за ID (Помилка - не знайдено)
	@Test
	void shouldReturnNotFoundWhenProductNotFound() throws Exception {
		mockMvc.perform(get("/products/999"))
				.andExpect(status().isNotFound());
	}

	// 4. POST (Успішне створення)
	@Test
	void shouldCreateNewProduct() throws Exception {
		Product newProduct = new Product(0, "Test Product", 100.0);
		mockMvc.perform(post("/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(newProduct)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Test Product"));
	}

	// 5. PUT (Успішне оновлення)
	@Test
	void shouldUpdateProduct() throws Exception {
		Product updatedProduct = new Product(1, "Updated Laptop", 1500.0);
		mockMvc.perform(put("/products/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updatedProduct)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated Laptop"));
	}

	// 6. PUT (Помилка - оновлення неіснуючого)
	@Test
	void shouldReturnNotFoundWhenUpdatingNonExisting() throws Exception {
		Product updatedProduct = new Product(999, "Ghost Product", 0.0);
		mockMvc.perform(put("/products/999")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updatedProduct)))
				.andExpect(status().isNotFound());
	}

	// 7. DELETE (Успішне видалення)
	@Test
	void shouldDeleteProduct() throws Exception {
		mockMvc.perform(delete("/products/2"))
				.andExpect(status().isNoContent());
	}

	// 8. DELETE (Помилка - видалення неіснуючого)
	@Test
	void shouldReturnNotFoundWhenDeletingNonExisting() throws Exception {
		mockMvc.perform(delete("/products/999"))
				.andExpect(status().isNotFound());
	}
}

