package com.yandex.reactive.testcontainers.testconainers_demo_raactive.service;

import com.yandex.reactive.testcontainers.testconainers_demo_raactive.domain.entity.Cart;
import com.yandex.reactive.testcontainers.testconainers_demo_raactive.domain.entity.Product;
import com.yandex.reactive.testcontainers.testconainers_demo_raactive.domain.entity.User;
import com.yandex.reactive.testcontainers.testconainers_demo_raactive.repository.CartRepository;
import com.yandex.reactive.testcontainers.testconainers_demo_raactive.repository.ProductRepository;
import com.yandex.reactive.testcontainers.testconainers_demo_raactive.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        // Если у юзера ещё нет корзины, , создать новую
        when(userRepository.findById(defaultUserId)).thenReturn(Mono.just(defaultUser));
        when(cartRepository.findByUserId(defaultUser.getId())).thenReturn(Mono.empty());

        var newCart = new Cart();
        newCart.setId(100L);
        newCart.setUserId(defaultUser.getId());
        newCart.setProducts(new ArrayList<>());
        newCart.setTotalPrice(0.0);
        when(cartRepository.save(any(Cart.class))).thenReturn(Mono.just(newCart));

        Mono<Cart> cartMono = cartService.getCartForUser(defaultUserId);

        StepVerifier.create(cartMono)
                .assertNext(cart -> assertThat(cart.getId()).isEqualTo(100L))
                .verifyComplete();

        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void testModifyItemPlus() {
        when(userRepository.findById(defaultUserId)).thenReturn(Mono.just(defaultUser));
        var cart = new Cart();
        cart.setId(1L);
        cart.setUserId(defaultUser.getId());
        cart.setProducts(new ArrayList<>());
        cart.setTotalPrice(0.0);
        when(cartRepository.findByUserId(defaultUser.getId())).thenReturn(Mono.just(cart));

        var product = new Product();
        product.setId(10L);
        product.setName("Test Product");
        product.setPrice(50.0);
        when(productRepository.findById(10L)).thenReturn(Mono.just(product));
        // Симуляция сохранения корзины – возвращаем ту же корзину
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<Cart> updatedCartMono = cartService.modifyItem(10L, "plus");

        StepVerifier.create(updatedCartMono)
                .assertNext(updatedCart -> {
                    assertThat(updatedCart.getProducts()).hasSize(1);
                    assertThat(updatedCart.getTotalPrice()).isEqualTo(50.0);
                })
                .verifyComplete();

        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void testModifyItemMinus() {
        when(userRepository.findById(defaultUserId)).thenReturn(Mono.just(defaultUser));
        var cart = new Cart();
        cart.setId(1L);
        cart.setUserId(defaultUser.getId());
        List<Product> products = new ArrayList<>();
        cart.setProducts(products);
        cart.setTotalPrice(0.0);
        when(cartRepository.findByUserId(defaultUser.getId())).thenReturn(Mono.just(cart));

        var product = new Product();
        product.setId(20L);
        product.setName("Test Product Minus");
        product.setPrice(30.0);
        when(productRepository.findById(20L)).thenReturn(Mono.just(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Добавляем два товара
        cartService.modifyItem(20L, "plus").block();
        cartService.modifyItem(20L, "plus").block();

        // Удаляем один экземпляр
        var updatedCart = cartService.modifyItem(20L, "minus").block();

        assertThat(updatedCart.getProducts()).hasSize(1);
        assertThat(updatedCart.getTotalPrice()).isEqualTo(30.0);
    }

    @Test
    void testModifyItemDelete() {
        // Если в корзине несколько экземпляров – action "delete" удаляет все
        when(userRepository.findById(defaultUserId)).thenReturn(Mono.just(defaultUser));
        var cart = new Cart();
        cart.setId(1L);
        cart.setUserId(defaultUser.getId());
        List<Product> products = new ArrayList<>();
        cart.setProducts(products);
        cart.setTotalPrice(0.0);
        when(cartRepository.findByUserId(defaultUser.getId())).thenReturn(Mono.just(cart));

        var product = new Product();
        product.setId(30L);
        product.setName("Test Product Delete");
        product.setPrice(40.0);
        when(productRepository.findById(30L)).thenReturn(Mono.just(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Добавляем товар три раза
        cartService.modifyItem(30L, "plus").block();
        cartService.modifyItem(30L, "plus").block();
        cartService.modifyItem(30L, "plus").block();
        assertThat(cart.getProducts()).hasSize(3);

        var updatedCart = cartService.modifyItem(30L, "delete").block();
        assertThat(updatedCart.getProducts()).isEmpty();
        assertThat(updatedCart.getTotalPrice()).isEqualTo(0.0);
    }

    @Test
    void testGetProductCounts() {
        when(userRepository.findById(defaultUserId)).thenReturn(Mono.just(defaultUser));
        var cart = new Cart();
        cart.setId(1L);
        cart.setUserId(defaultUser.getId());

        var item1 = new Product();
        item1.setId(100L);
        item1.setName("Prod1");
        item1.setPrice(10.0);
        item1.setDescription("Desc");
        item1.setImgPath("img.jpg");

        var item2 = new Product();
        item2.setId(200L);
        item2.setName("Prod2");
        item2.setPrice(20.0);
        item2.setDescription("Desc2");
        item2.setImgPath("img2.jpg");

        List<Product> products = List.of(item1, item1, item2);
        cart.setProducts(products);
        cart.setTotalPrice(40.0);
        when(cartRepository.findByUserId(defaultUser.getId())).thenReturn(Mono.just(cart));

        Mono<Map<Long, Integer>> countsMono = cartService.getProductCounts();

        StepVerifier.create(countsMono)
                .assertNext(counts -> {
                    assertThat(counts).hasSize(2);
                    assertThat(counts.get(100L)).isEqualTo(2);
                    assertThat(counts.get(200L)).isEqualTo(1);
                })
                .verifyComplete();
    }
}
