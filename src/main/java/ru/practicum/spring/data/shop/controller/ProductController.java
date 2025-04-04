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
import ru.practicum.spring.data.shop.dto.ProductDTO;
import ru.practicum.spring.data.shop.exception.ResourceNotFoundException;
import ru.practicum.spring.data.shop.service.CartService;
import ru.practicum.spring.data.shop.service.ProductService;

import java.util.List;
import java.util.stream.Collectors;

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
        groupedItems.forEach(row ->
            row.forEach(p -> p.setCount(counts.getOrDefault(p.getId(), 0)))
        );

        // Convert entity list to DTO list
        List<List<ProductDTO>> groupedDTOs = groupedItems.stream()
                .map(row -> row.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList())
                )
                .collect(Collectors.toList());

        model.addAttribute("items", groupedDTOs);
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);

        Paging paging = new Paging(pageNumber, pageSize, productPage.hasNext(), productPage.hasPrevious());
        model.addAttribute("paging", paging);

        return "main";
    }

    // POST "/main/items/{id}" – изменить количество товара в корзине с main страницы
    @PostMapping("/main/items/{id}")
    public String modifyMainItems(@PathVariable Long id, @RequestParam("action") String action) {
        cartService.modifyItem(id, action);
        return "redirect:/main/items";
    }

    // Новый эндпоинт: GET "/items/{id}" – отображение деталей продукта
    @GetMapping("/items/{id}")
    public String getSingleItem(@PathVariable Long id, Model model) {
        var product = productService.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Продукт с id " + id + " не найден")
        );

        // Fetch cart counts and update the product count field
        var counts = cartService.getProductCounts();
        product.setCount(counts.getOrDefault(product.getId(), 0));
        ProductDTO dto = convertToDTO(product);
        model.addAttribute("item", dto);
        return "item";
    }

    private ProductDTO convertToDTO(Product product) {
        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getImgPath(),
                product.getCount()
        );
    }
}