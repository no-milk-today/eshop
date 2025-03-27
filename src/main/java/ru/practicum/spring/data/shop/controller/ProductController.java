package ru.practicum.spring.data.shop.controller;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.spring.data.shop.domain.entity.Product;
import ru.practicum.spring.data.shop.dto.Paging;
import ru.practicum.spring.data.shop.service.ProductService;

import java.util.List;

@Controller
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
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

        model.addAttribute("items", groupedItems);
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);

        Paging paging = new Paging(pageNumber, pageSize, productPage.hasNext(), productPage.hasPrevious());
        model.addAttribute("paging", paging);

        return "main"; // Шаблон main.html
    }
}