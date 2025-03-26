package ru.practicum.spring.data.shop.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.practicum.spring.data.shop.domain.entity.Order;
import ru.practicum.spring.data.shop.service.OrderService;

import java.util.Optional;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public String getOrder(@PathVariable Long id, Model model, HttpServletResponse response) {
        Optional<Order> orderOpt = orderService.findById(id);
        response.setContentType("text/html;charset=UTF-8"); //todo: to be refactored
        if (orderOpt.isEmpty()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "not-found"; // Страница not-found.html
        }

        model.addAttribute("order", orderOpt.get());
        return "order"; // Страница с описанием заказа order.html
    }

    @PostMapping
    public String createOrder(@ModelAttribute Order order) {
        orderService.save(order);

        return "redirect:/orders"; // Редирект на страницу с описанием заказов
    }

    @DeleteMapping("/{id}")
    public String deleteOrder(@PathVariable Long id) {
        orderService.deleteById(id);
        return "redirect:/orders"; // Редирект на страницу с описанием заказов
    }

}
