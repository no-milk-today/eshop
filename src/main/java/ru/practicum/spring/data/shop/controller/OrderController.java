package ru.practicum.spring.data.shop.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.practicum.spring.data.shop.domain.entity.Order;
import ru.practicum.spring.data.shop.domain.entity.Product;
import ru.practicum.spring.data.shop.exception.ResourceNotFoundException;
import ru.practicum.spring.data.shop.dto.OrderDTO;
import ru.practicum.spring.data.shop.service.OrderProcessingService;
import ru.practicum.spring.data.shop.service.OrderService;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final OrderProcessingService orderProcessingService;

    public OrderController(OrderService orderService, OrderProcessingService orderProcessingService) {
        this.orderService = orderService;
        this.orderProcessingService = orderProcessingService;
    }

    // POST "/buy" – buy products in the cart, create an order, clear cart and redirect to order details
    @PostMapping("/buy")
    public String buy() {
        Order order = orderProcessingService.processOrder();
        return "redirect:/orders/" + order.getId() + "?newOrder=true";
    }

    @GetMapping("/orders/{id}")
    public String getOrder(@PathVariable Long id,
                           @RequestParam(name = "newOrder", required = false, defaultValue = "false") boolean newOrder,
                           Model model, HttpServletResponse response) {
        Order orderFromDB = orderService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id [" + id + "] not found"));

        // fetch unique products with count
        List<Product> groupedProducts = groupProductsWithCounts(orderFromDB.getProducts());
        orderFromDB.setProducts(groupedProducts);

        double totalSum = orderService.calculateTotalSum(orderFromDB);
        orderFromDB.setTotalSum(totalSum);

        response.setContentType("text/html;charset=UTF-8");
        OrderDTO orderDTO = convertToDTO(orderFromDB);
        model.addAttribute("order", orderDTO);
        model.addAttribute("newOrder", newOrder);
        return "order";
    }

    /**
     * Фетчит уникальные продукты и их количество
     *
     * @param products initial products
     * @return уникальные продукты и их количество
     */
    private List<Product> groupProductsWithCounts(List<Product> products) {
        Map<Long, Integer> productCounts = products.stream()
                .collect(Collectors.groupingBy(Product::getId, Collectors.summingInt(p -> 1)));

        return products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> p,
                        (p1, p2) -> p1 // сохраняем первый экземпляр
                ))
                .values().stream()
                .peek(p -> p.setCount(productCounts.getOrDefault(p.getId(), 0)))
                .collect(Collectors.toList());
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