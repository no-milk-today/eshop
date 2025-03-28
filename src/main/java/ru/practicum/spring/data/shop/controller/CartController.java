package ru.practicum.spring.data.shop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.practicum.spring.data.shop.domain.entity.Product;
import ru.practicum.spring.data.shop.dto.CartItemDto;
import ru.practicum.spring.data.shop.service.CartService;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cart/items")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // GET "/cart/items" – отображение корзины
    @GetMapping
    public String getCart(Model model) {
        var cart = cartService.getCart();
        var counts = cartService.getProductCounts();

        // Получаем уникальные товары по id
        var uniqueProducts = cart.getProducts().stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> p,
                        (p1, p2) -> p1  // сохраняем первый экземпляр
                ))
                .values();

        List<CartItemDto> productDtos = uniqueProducts.stream()
                .map(p -> CartItemDto.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .imgPath(p.getImgPath())
                        .price(p.getPrice())
                        .count(counts.getOrDefault(p.getId(), 0))
                        .build())
                .collect(Collectors.toList());

        model.addAttribute("products", productDtos);
        model.addAttribute("total", cart.getTotalPrice());
        boolean empty = cart.getProducts().isEmpty();
        model.addAttribute("empty", empty);
        return "cart"; // Шаблон cart.html
    }

    // POST "/cart/items/{id}" – изменение количества товара в корзине
    @PostMapping("/{id}")
    public String modifyCartItem(@PathVariable Long id, @RequestParam("action") String action) {
        cartService.modifyItem(id, action);
        return "redirect:/cart/items";
    }
}