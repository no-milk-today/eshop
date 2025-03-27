package ru.practicum.spring.data.shop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.spring.data.shop.domain.entity.Cart;
import ru.practicum.spring.data.shop.domain.entity.Product;
import ru.practicum.spring.data.shop.domain.entity.User;
import ru.practicum.spring.data.shop.repository.CartRepository;
import ru.practicum.spring.data.shop.repository.ProductRepository;
import ru.practicum.spring.data.shop.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private final Long defaultUserId = 1L;
    private User defaultUser;

    @BeforeEach
    void setUp() {
        defaultUser = User.builder()
                .id(defaultUserId)
                .username("default")
                .password("pass")
                .email("default@example.com")
                .build();
    }

    @Test
    void testGetCartForNewUser() {
        // Если у юзера ещё нет корзины, создать новую
        when(userRepository.findById(defaultUserId)).thenReturn(Optional.of(defaultUser));
        when(cartRepository.findByUser(defaultUser)).thenReturn(Optional.empty());

        var newCart = new Cart();
        newCart.setId(100L);
        newCart.setUser(defaultUser);
        newCart.setProducts(new ArrayList<>());
        newCart.setTotalPrice(0.0);
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        var cart = cartService.getCartForUser(defaultUserId);
        assertThat(cart.getId()).isEqualTo(100L);

        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void testModifyItemPlus() {
        when(userRepository.findById(defaultUserId)).thenReturn(Optional.of(defaultUser));
        var cart = new Cart();
        cart.setId(1L);
        cart.setUser(defaultUser);
        cart.setProducts(new ArrayList<>());
        cart.setTotalPrice(0.0);
        when(cartRepository.findByUser(defaultUser)).thenReturn(Optional.of(cart));

        var product = new Product();
        product.setId(10L);
        product.setName("Test Product");
        product.setPrice(50.0);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        cartService.modifyItem(10L, "plus");
        assertThat(cart.getProducts()).hasSize(1);
        assertThat(cart.getTotalPrice()).isEqualTo(50.0);

        verify(cartRepository).save(cart);
    }

    @Test
    void testModifyItemMinus() {
        when(userRepository.findById(defaultUserId)).thenReturn(Optional.of(defaultUser));
        var cart = new Cart();
        cart.setId(1L);
        cart.setUser(defaultUser);
        List<Product> products = new ArrayList<>();
        cart.setProducts(products);
        cart.setTotalPrice(0.0);
        when(cartRepository.findByUser(defaultUser)).thenReturn(Optional.of(cart));

        var product = new Product();
        product.setId(20L);
        product.setName("Test Product Minus");
        product.setPrice(30.0);
        when(productRepository.findById(20L)).thenReturn(Optional.of(product));

        cartService.modifyItem(20L, "plus");
        cartService.modifyItem(20L, "plus");
        assertThat(cart.getProducts()).hasSize(2);
        // minus one instance
        cartService.modifyItem(20L, "minus");
        assertThat(cart.getProducts()).hasSize(1);
        assertThat(cart.getTotalPrice()).isEqualTo(30.0);
    }

    @Test
    void testModifyItemDelete() {
        // Если в корзине несколько экземпляров, action "delete" удаляет ВСЕ
        when(userRepository.findById(defaultUserId)).thenReturn(Optional.of(defaultUser));
        var cart = new Cart();
        cart.setId(1L);
        cart.setUser(defaultUser);
        List<Product> products = new ArrayList<>();
        cart.setProducts(products);
        cart.setTotalPrice(0.0);
        when(cartRepository.findByUser(defaultUser)).thenReturn(Optional.of(cart));

        var product = new Product();
        product.setId(30L);
        product.setName("Test Product Delete");
        product.setPrice(40.0);
        when(productRepository.findById(30L)).thenReturn(Optional.of(product));

        cartService.modifyItem(30L, "plus");
        cartService.modifyItem(30L, "plus");
        cartService.modifyItem(30L, "plus");
        assertThat(cart.getProducts()).hasSize(3);

        cartService.modifyItem(30L, "delete");
        assertThat(cart.getProducts()).isEmpty();
        assertThat(cart.getTotalPrice()).isEqualTo(0.0);
    }

    @Test
    void testGetProductCounts() {
        when(userRepository.findById(defaultUserId)).thenReturn(Optional.of(defaultUser));
        var cart = new Cart();
        cart.setId(1L);
        cart.setUser(defaultUser);

        var item1 = new Product(100L, "Prod1", 10.0, "Desc", "img.jpg", 0);
        var item2 = new Product(200L, "Prod2", 20.0, "Desc2", "img2.jpg", 0);
        var products = List.of(
                item1,
                item1,
                item2
        );

        cart.setProducts(products);
        cart.setTotalPrice(40.0);
        when(cartRepository.findByUser(defaultUser)).thenReturn(Optional.of(cart));

        var counts = cartService.getProductCounts();
        assertThat(counts).hasSize(2);
        assertThat(counts.get(100L)).isEqualTo(2);
        assertThat(counts.get(200L)).isEqualTo(1);
    }
}