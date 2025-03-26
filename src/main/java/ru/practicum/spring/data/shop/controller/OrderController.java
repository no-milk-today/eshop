package ru.practicum.spring.data.shop.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.practicum.spring.data.shop.domain.entity.Order;
import ru.practicum.spring.data.shop.service.OrderService;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // GET "/orders/{id}" - карточка заказа
    @GetMapping("/{id}")
    public String getOrder(@PathVariable Long id,
                           @RequestParam(name = "newOrder", required = false, defaultValue = "false") boolean newOrder,
                           Model model, HttpServletResponse response) {
        Optional<Order> orderFromDB = orderService.findById(id);
        response.setContentType("text/html;charset=UTF-8"); //todo: to be refactored
        if (orderFromDB.isEmpty()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "not-found"; // Страница not-found.html
        }
        model.addAttribute("order", orderFromDB.get());
        model.addAttribute("newOrder", newOrder);
        return "order"; // Страница order.html
    }

    // GET "/orders" - список заказов (с возможной сортировкой)
    @GetMapping
    public String orders(@RequestParam(value = "sortBy", required = false) String sortBy, Model model) {
        List<Order> orders;
        if (sortBy != null && !sortBy.isBlank()) {
            orders = orderService.findAllSorted(Sort.by(Sort.Direction.ASC, sortBy));
        } else {
            orders = orderService.findAll();
        }
        model.addAttribute("orders", orders);
        return "orders-list"; // Шаблон orders-list.html
    }

    @PostMapping
    public String createOrder(@ModelAttribute Order order) {
        orderService.save(order);
        return "redirect:/orders"; // Редирект после создания заказа
    }

    @DeleteMapping("/{id}")
    public String deleteOrder(@PathVariable Long id) {
        orderService.deleteById(id);
        return "redirect:/orders"; // Редирект после удаления заказа
    }
}