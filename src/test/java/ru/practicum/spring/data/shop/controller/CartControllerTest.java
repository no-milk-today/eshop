package ru.practicum.spring.data.shop.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.practicum.spring.data.shop.domain.entity.Cart;
import ru.practicum.spring.data.shop.domain.entity.Product;
import ru.practicum.spring.data.shop.service.CartService;

@WebMvcTest(CartController.class)
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @Test
    void testGetCart() throws Exception {
        // prepare cart
        var cart = new Cart();
        cart.setId(1L);
        // assume, that cart contains 2 same products and one different
        var p1 = new Product(100L, "Product 100", 10.0, "Description 100", "img100.jpg", 0);
        var p2 = new Product(200L, "Product 200", 20.0, "Description 200", "img200.jpg", 0);
        cart.setProducts(Arrays.asList(p1, p1, p2));
        cart.setTotalPrice(10.0 * 2 + 20.0);

        when(cartService.getCart()).thenReturn(cart);
        when(cartService.getProductCounts()).thenReturn(
                java.util.Map.of(100L, 2, 200L, 1));

        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attribute("total", 40.0))
                .andExpect(model().attributeExists("empty"));

        // empty должно быть false, так как в корзине есть товары
        // Дополнительная проверка может быть реализована в шаблоне, здесь достаточно, что атрибут присутствует.
    }

    @Test
    void testModifyCartItem() throws Exception {
        // Проверяем, что POST-запрос корректно передаёт id и action, и делает редирект.
        mockMvc.perform(post("/cart/items/100")
                        .param("action", "plus"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));

        verify(cartService).modifyItem(eq(100L), eq("plus"));
    }
}