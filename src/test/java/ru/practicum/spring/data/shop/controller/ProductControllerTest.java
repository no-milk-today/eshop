package ru.practicum.spring.data.shop.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.spring.data.shop.domain.entity.Product;
import ru.practicum.spring.data.shop.dto.ProductDTO;
import ru.practicum.spring.data.shop.service.CartService;
import ru.practicum.spring.data.shop.service.ProductService;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CartService cartService;

    @Test
    void testIndexRedirect() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main/items"));
    }

    @Test
    void testMainItems() throws Exception {
        // данные для страницы витрины
        var products = List.of(new Product(), new Product(), new Product(), new Product());
        var page = new PageImpl<>(products, PageRequest.of(0, 10), products.size());
        when(productService.getProducts("", "NO", 1, 10)).thenReturn(page);
        when(productService.groupProducts(products)).thenReturn(
                List.of(List.of(new Product(), new Product()), List.of(new Product(), new Product()))
        );
        when(cartService.getProductCounts()).thenReturn(Collections.emptyMap());

        mockMvc.perform(get("/main/items")
                        .param("search", "")
                        .param("sort", "NO")
                        .param("pageSize", "10")
                        .param("pageNumber", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("search"))
                .andExpect(model().attributeExists("sort"))
                .andExpect(model().attributeExists("paging"));

        verify(productService).getProducts("", "NO", 1, 10);
    }

    @Test
    void testGetSingleItemFound() throws Exception {
        var product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setDescription("Description");
        product.setPrice(123.45);
        product.setImgPath("images/test.jpg");
        product.setCount(0);

        when(productService.findById(1L)).thenReturn(Optional.of(product));
        when(cartService.getProductCounts()).thenReturn(Collections.emptyMap());

        var expectedDto = new ProductDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getImgPath(),
                0
        );

        // Act & Assert
        mockMvc.perform(get("/items/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"))
                .andExpect(model().attribute("item", expectedDto));
    }
}