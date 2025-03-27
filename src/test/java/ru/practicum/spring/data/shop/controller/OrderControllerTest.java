package ru.practicum.spring.data.shop.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.spring.data.shop.domain.entity.Order;
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

    @MockitoBean // Подменяем реализацию сервиса, чтобы управлять поведением
    OrderService orderService;

    // Тест для GET "/orders/{id}" без параметра newOrder (по умолчанию false)
    @Test
    void testFindByIdFound() throws Exception {
        var order = new Order();
        order.setId(3L);
        order.setNumber("#123456");
        order.setOrderDate(LocalDateTime.now());
        doReturn(Optional.of(order)).when(orderService).findById(3L);

        mockMvc.perform(get("/orders/3"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(view().name("order"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attribute("newOrder", false));
    }

    // Тест для GET "/orders/{id}" когда заказ не найден
    @Test
    void testFindByIdNotFound() throws Exception {
        doReturn(Optional.empty()).when(orderService).findById(2L);

        mockMvc.perform(get("/orders/2"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("not-found"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage", "Заказ с id [2] не найден"));
    }

    // Тест для GET "/orders/{id}" с параметром newOrder=true
    @Test
    void testGetOrderWithNewOrderParameter() throws Exception {
        var order = new Order();
        order.setId(5L);
        order.setNumber("#7890");
        order.setOrderDate(LocalDateTime.now());
        doReturn(Optional.of(order)).when(orderService).findById(5L);

        mockMvc.perform(get("/orders/5").param("newOrder", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attribute("newOrder", true));
    }

    // Тест для GET "/orders" без параметра сортировки – используется orderService.findAll()
    @Test
    void testOrdersListWithoutSortParam() throws Exception {
        var order1 = new Order();
        order1.setId(10L);
        var order2 = new Order();
        order2.setId(11L);
        doReturn(List.of(order1, order2)).when(orderService).findAll();

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders-list"))
                .andExpect(model().attributeExists("orders"));
        verify(orderService).findAll();
    }

    // Тест для GET "/orders" с параметром sortBy – используется orderService.findAllSorted(...)
    @Test
    void testFindAllSortedOrders() throws Exception {
        var order1 = new Order();
        order1.setId(1L);
        order1.setNumber("#100");

        var order2 = new Order();
        order2.setId(2L);
        order2.setNumber("#101");

        // Мокаем вызов метода сортировки; любой переданный параметр Sort удовлетворяет условию any()
        doReturn(List.of(order1, order2)).when(orderService).findAllSorted(any());

        // Выполняем запрос с параметром sortBy, в данном случае "number"
        mockMvc.perform(get("/orders").param("sortBy", "number"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders-list"))
                .andExpect(model().attributeExists("orders"));
        verify(orderService).findAllSorted(any());
    }

    // Тест для создания заказа
    @Test
    void testCreateOrder() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .queryParam("id", "3")
                        .queryParam("number", "#654321")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, "/orders"));

        verify(orderService).save(any());
    }

    // Тест для удаления заказа
    @Test
    void testDeleteOrder() throws Exception {
        mockMvc.perform(delete("/orders/5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().stringValues(HttpHeaders.LOCATION, "/orders"));

        verify(orderService).deleteById(5L);
    }
}