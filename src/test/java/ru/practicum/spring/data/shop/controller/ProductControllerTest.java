package ru.practicum.spring.data.shop.controller;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.spring.data.shop.domain.entity.Product;
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
        List<Product> products = List.of(new Product(), new Product(), new Product(), new Product());
        Page<Product> page = new PageImpl<>(products, PageRequest.of(0, 10), products.size());
        doReturn(page).when(productService).getProducts("", "NO", 1, 10);
        // Для группировки имитируем две строки по два продукта
        doReturn(List.of(List.of(new Product(), new Product()), List.of(new Product(), new Product())))
                .when(productService).groupProducts(products);

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
}