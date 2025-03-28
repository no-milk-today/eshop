package ru.practicum.spring.data.shop.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.practicum.spring.data.shop.domain.entity.Order;
import ru.practicum.spring.data.shop.exception.ResourceNotFoundException;
import ru.practicum.spring.data.shop.service.CartService;
import ru.practicum.spring.data.shop.service.OrderService;
import ru.practicum.spring.data.shop.dto.OrderDTO;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    public OrderController(OrderService orderService, CartService cartService) {
        this.orderService = orderService;
        this.cartService = cartService;
    }

    // POST "/buy" – buy products in the cart, create an order, clear cart and redirect to order details
    @PostMapping("/buy")
    public String buy() {
        var cart = cartService.getCart();

        if (cart.getProducts().isEmpty()) {
            return "redirect:/cart/items";
        }

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setProducts(List.copyOf(cart.getProducts()));
        order.setTotalSum(cart.getTotalPrice());
        order.setOrderDate(LocalDateTime.now());
        order = orderService.save(order);
        cartService.clearCart();
        return "redirect:/orders/" + order.getId() + "?newOrder=true";
    }

    // GET "/orders/{id}" - order details
    @GetMapping("/orders/{id}")
    public String getOrder(@PathVariable Long id,
                           @RequestParam(name = "newOrder", required = false, defaultValue = "false") boolean newOrder,
                           Model model, HttpServletResponse response) {
        Order orderFromDB = orderService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id [" + id + "] not found"));
        double totalSum = orderService.calculateTotalSum(orderFromDB);
        orderFromDB.setTotalSum(totalSum);

        response.setContentType("text/html;charset=UTF-8");
        OrderDTO orderDTO = convertToDTO(orderFromDB);
        model.addAttribute("order", orderDTO);
        model.addAttribute("newOrder", newOrder);
        return "order";
    }

    // GET "/orders" - list orders
    @GetMapping("/orders")
    public String orders(@RequestParam(value = "sortBy", required = false) String sortBy, Model model) {
        List<Order> orders;
        if (sortBy != null && !sortBy.isBlank()) {
            orders = orderService.findAllSorted(Sort.by(Sort.Direction.ASC, sortBy));
        } else {
            orders = orderService.findAll();
        }
        List<OrderDTO> orderDTOs = orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        model.addAttribute("orders", orderDTOs);
        return "orders-list";
    }

    private OrderDTO convertToDTO(Order order) {
        return new OrderDTO(
                order.getId(),
                order.getUser().getId(),
                order.getProducts(),
                order.getOrderDate(),
                order.getNumber(),
                order.getTotalSum()
        );
    }

    @DeleteMapping("/orders/{id}")
    public String deleteOrder(@PathVariable Long id) {
        orderService.deleteById(id);
        return "redirect:/orders";
    }
}