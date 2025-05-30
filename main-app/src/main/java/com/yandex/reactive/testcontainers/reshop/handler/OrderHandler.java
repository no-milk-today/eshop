package com.yandex.reactive.testcontainers.reshop.handler;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Order;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.dto.OrderDTO;
import com.yandex.reactive.testcontainers.reshop.exception.PaymentException;
import com.yandex.reactive.testcontainers.reshop.service.OrderProcessingService;
import com.yandex.reactive.testcontainers.reshop.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OrderHandler {

    private final OrderService orderService;
    private final OrderProcessingService orderProcessingService;

    public OrderHandler(OrderService orderService, OrderProcessingService orderProcessingService) {
        this.orderService = orderService;
        this.orderProcessingService = orderProcessingService;
    }

    /**
     * POST "/buy" – Покупка товаров из корзины: создание заказа, очистка корзины и редирект на детали заказа.
     * В случае payment failure, рендеринг payment-error темплейта.
     */
    public Mono<ServerResponse> buy(ServerRequest request) {
        return orderProcessingService.processOrder()
                .flatMap(order ->
                        ServerResponse.seeOther(URI.create("/orders/" + order.getId() + "?newOrder=true")).build()
                )
                .onErrorResume(PaymentException.class, ex -> {
                    Map<String, Object> model = Map.of("errorMessage", ex.getMessage());
                    return ServerResponse.status(HttpStatus.PAYMENT_REQUIRED).render("payment-error", model);
                });
    }

    /**
     * GET "/orders/{id}" – отображение деталей заказа.
     */
    public Mono<ServerResponse> getOrder(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        boolean newOrder = Boolean.parseBoolean(request.queryParam("newOrder").orElse("false"));
        Model model = new ConcurrentModel();

        return orderService.findByIdWithProducts(id)
                .flatMap(order -> {
                    List<Product> groupedProducts = orderService.groupProductsWithCounts(order.getProducts());
                    order.setProducts(groupedProducts);
                    double totalSum = orderService.calculateTotalSum(order);
                    order.setTotalSum(totalSum);
                    OrderDTO orderDTO = convertToDTO(order);
                    model.addAttribute("order", orderDTO);
                    model.addAttribute("newOrder", newOrder);
                    return ServerResponse.ok().render("order", model.asMap());
                })
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * GET "/orders" – отображение списка заказов для текущего юзера.
     */
    public Mono<ServerResponse> orders(ServerRequest request) {
        var authenticationMono = request.principal()
                .cast(Authentication.class)
                .filter(auth -> auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken))
                .switchIfEmpty(Mono.empty());
        // Extract username from authentication
        var usernameMono = authenticationMono
                .map(auth -> {
                    if (auth.getPrincipal() instanceof String) {
                        return (String) auth.getPrincipal();
                    } else if (auth.getPrincipal() instanceof OidcUser) {
                        return ((OidcUser) auth.getPrincipal()).getPreferredUsername();
                    } else {
                        return "Гость"; // fallback for unknown principal type
                    }
                })
                .defaultIfEmpty("Гость");

        var model = new ConcurrentModel();

        var ordersForUser = usernameMono
                .flatMapMany(orderService::findOrdersForUsername);

        return ordersForUser
                .flatMap(order -> orderService.findByIdWithProducts(order.getId()))
                .collectList()
                .flatMap(orders -> {
                    orders.forEach(order -> {
                        List<Product> groupedProducts = orderService.groupProductsWithCounts(order.getProducts());
                        order.setProducts(groupedProducts);
                        double totalSum = orderService.calculateTotalSum(order);
                        order.setTotalSum(totalSum);
                    });
                    List<OrderDTO> orderDTOs = orders.stream()
                            .map(this::convertToDTO)
                            .collect(Collectors.toList());
                    model.addAttribute("orders", orderDTOs);
                    return ServerResponse.ok().render("orders-list", model.asMap());
                });
    }

    private OrderDTO convertToDTO(Order order) {
        return new OrderDTO(
                order.getId(),
                order.getUserId(),
                order.getProducts(),
                order.getOrderDate(),
                order.getNumber(),
                order.getTotalSum()
        );
    }
}

