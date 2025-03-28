package ru.practicum.spring.data.shop.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.spring.data.shop.domain.entity.Cart;
import ru.practicum.spring.data.shop.domain.entity.Order;
import ru.practicum.spring.data.shop.domain.entity.Product;
import ru.practicum.spring.data.shop.domain.entity.User;
import ru.practicum.spring.data.shop.service.CartService;
import ru.practicum.spring.data.shop.service.OrderService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CartService cartService;

    // Helper method to build an Order with a default User
    private Order buildOrder(Long id, String number) {
        var order = new Order();
        order.setId(id);
        order.setNumber(number);
        order.setOrderDate(LocalDateTime.now());
        // Set a default user
        var user = new User();
        user.setId(1L);
        order.setUser(user);
        order.setTotalSum(100.0);
        order.setProducts(List.of());
        return order;
    }

    // GET "/orders/{id}" without newOrder parameter
    @Test
    void testFindByIdFound() throws Exception {
        var order = buildOrder(3L, "#123456");
        doReturn(Optional.of(order)).when(orderService).findById(3L);

        mockMvc.perform(get("/orders/3"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(view().name("order"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attribute("newOrder", false));
    }

    // GET "/orders/{id}" when order is not found
    @Test
    void testFindByIdNotFound() throws Exception {
        doReturn(Optional.empty()).when(orderService).findById(2L);

        mockMvc.perform(get("/orders/2"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("not-found"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage", "Order with id [2] not found"));
    }

    // GET "/orders/{id}" with newOrder parameter set to true
    @Test
    void testGetOrderWithNewOrderParameter() throws Exception {
        var order = buildOrder(5L, "#7890");
        doReturn(Optional.of(order)).when(orderService).findById(5L);

        mockMvc.perform(get("/orders/5").param("newOrder", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attribute("newOrder", true));
    }

    // GET "/orders" without a sort parameter
    @Test
    void testOrdersListWithoutSortParam() throws Exception {
        var order1 = buildOrder(10L, "#10");
        var order2 = buildOrder(11L, "#11");
        doReturn(List.of(order1, order2)).when(orderService).findAll();

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders-list"))
                .andExpect(model().attributeExists("orders"));
        verify(orderService).findAll();
    }

    // GET "/orders" with a sortBy parameter
    @Test
    void testFindAllSortedOrders() throws Exception {
        var order1 = buildOrder(1L, "#100");
        var order2 = buildOrder(2L, "#101");
        doReturn(List.of(order1, order2)).when(orderService).findAllSorted(any());

        mockMvc.perform(get("/orders").param("sortBy", "number"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders-list"))
                .andExpect(model().attributeExists("orders"));
        verify(orderService).findAllSorted(any());
    }

    // POST "/buy" endpoint for creating an order and clearing the cart
    @Test
    void testBuyOrder() throws Exception {
        var user = new User();
        user.setId(1L);
        var cart = new Cart();
        cart.setUser(user);
        cart.setProducts(List.of(new Product(1L, "Test Product", 10.0, "Description", "img.jpg", 0)));
        cart.setTotalPrice(10.0);

        doReturn(cart).when(cartService).getCart();

        var order = buildOrder(7L, "#777");
        doReturn(order).when(orderService).save(any());

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/7?newOrder=true"));
        verify(orderService).save(any());
    }

    // DELETE "/orders/{id}" endpoint for deleting an order
    @Test
    void testDeleteOrder() throws Exception {
        mockMvc.perform(delete("/orders/5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, "/orders"));
        verify(orderService).deleteById(5L);
    }
}