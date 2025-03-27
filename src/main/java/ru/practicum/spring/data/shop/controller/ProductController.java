package ru.practicum.spring.data.shop.controller;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.spring.data.shop.domain.entity.Product;
import ru.practicum.spring.data.shop.dto.Paging;
import ru.practicum.spring.data.shop.service.CartService;
import ru.practicum.spring.data.shop.service.ProductService;

import java.util.List;

@Controller
public class ProductController {

    private final ProductService productService;
    private final CartService cartService;

    public ProductController(ProductService productService, CartService cartService) {
        this.productService = productService;
        this.cartService = cartService;
    }

    // GET "/" – редирект на "/main/items"
    @GetMapping("/")
    public String index() {
        return "redirect:/main/items";
    }

    // GET "/main/items" – витрина продуктов
    @GetMapping("/main/items")
    public String mainItems(
            @RequestParam(name = "search", defaultValue = "") String search,
            @RequestParam(name = "sort", defaultValue = "NO") String sort,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "pageNumber", defaultValue = "1") int pageNumber,
            Model model) {

        Page<Product> productPage = productService.getProducts(search, sort, pageNumber, pageSize);
        List<Product> products = productPage.getContent();

        // Группируем товары для плиточного отображения (например, по 3 в ряд)
        List<List<Product>> groupedItems = productService.groupProducts(products);

        // cart info
        var counts = cartService.getProductCounts();
        // Проставляем для каждого продукта его count (если не найден — 0)
        groupedItems.forEach(row -> row.forEach(p -> p.setCount(counts.getOrDefault(p.getId(), 0))));

        model.addAttribute("items", groupedItems);
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);

        Paging paging = new Paging(pageNumber, pageSize, productPage.hasNext(), productPage.hasPrevious());
        model.addAttribute("paging", paging);

        return "main"; // Шаблон main.html
    }

    // POST "/main/items/{id}" – изменить количество товара в корзине с main страницы
    @PostMapping("/main/items/{id}")
    public String modifyMainItems(@PathVariable Long id, @RequestParam("action") String action) {
        cartService.modifyItem(id, action);
        return "redirect:/main/items";
    }
}