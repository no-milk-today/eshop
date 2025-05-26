package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Cart;
import com.yandex.reactive.testcontainers.reshop.domain.entity.CartProduct;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.domain.entity.User;
import com.yandex.reactive.testcontainers.reshop.repository.CartProductRepository;
import com.yandex.reactive.testcontainers.reshop.repository.CartRepository;
import com.yandex.reactive.testcontainers.reshop.repository.ProductRepository;
import com.yandex.reactive.testcontainers.reshop.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartProductRepository cartProductRepository;

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
        // newCart.setProducts(new ArrayList<>());
        newCart.setTotalPrice(0.0);
        when(cartRepository.save(any(Cart.class))).thenReturn(Mono.just(newCart));

        var cart = cartService.getCartForUser(defaultUserId).block();

        assertThat(cart)
                .withFailMessage("Cart should not be null")
                .isNotNull()
                .withFailMessage("Cart ID should be 100")
                .extracting(Cart::getId)
                .isEqualTo(100L);

        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void testModifyItemPlus() {
        var cart = new Cart();
        cart.setId(200L);
        cart.setUserId(defaultUser.getId());
        // cart.setProducts(new ArrayList<>());
        cart.setTotalPrice(0.0);

        var product = new Product();
        product.setId(10L);
        product.setName("Test Product");
        product.setPrice(50.0);

        // Use findByUsername for authenticated user lookup
        when(userRepository.findByUsername("default")).thenReturn(Mono.just(defaultUser));
        when(cartRepository.findByUserId(defaultUser.getId())).thenReturn(Mono.just(cart));
        when(productRepository.findById(product.getId())).thenReturn(Mono.just(product));
        when(cartProductRepository.save(any(CartProduct.class))).thenAnswer(invocation -> {
            CartProduct cp = invocation.getArgument(0);
            cp.setId(1L);
            return Mono.just(cp);
        });
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(cartProductRepository.findByCartId(cart.getId())).thenReturn(Flux.just(new CartProduct(1L, cart.getId(), product.getId())));

        var auth = new UsernamePasswordAuthenticationToken("default", "pass");
        cartService.modifyItem(product.getId(), "plus")
                   .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                   .block();

        verify(cartProductRepository).save(any());
        verify(cartRepository, atLeastOnce()).save(any(Cart.class));
    }

    @Test
    void testModifyItemMinus() {
        // корзина, product и наличие CartProduct для удаления
        var cart = new Cart();
        cart.setId(300L);
        cart.setUserId(defaultUser.getId());
        cart.setTotalPrice(100.0);

        var product = new Product();
        product.setId(20L);
        product.setName("Another Product");
        product.setPrice(50.0);

        var existingCP = new CartProduct(2L, cart.getId(), product.getId());

        when(userRepository.findByUsername("default")).thenReturn(Mono.just(defaultUser));
        when(cartRepository.findByUserId(defaultUser.getId())).thenReturn(Mono.just(cart));
        when(productRepository.findById(product.getId())).thenReturn(Mono.just(product));
        when(cartProductRepository.findFirstByCartIdAndProductId(cart.getId(), product.getId()))
                .thenReturn(Mono.just(existingCP));
        when(cartProductRepository.deleteById(existingCP.getId())).thenReturn(Mono.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // После удаления возвращаем пустой список, что приведёт к totalPrice = 0.0
        when(cartProductRepository.findByCartId(cart.getId())).thenReturn(Flux.empty());

        var auth = new UsernamePasswordAuthenticationToken("default", "pass");
        cartService.modifyItem(product.getId(), "minus")
                   .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                   .block();

        verify(cartProductRepository).findFirstByCartIdAndProductId(cart.getId(), product.getId());
        verify(cartProductRepository).deleteById(existingCP.getId());
        verify(cartRepository, atLeastOnce()).save(any(Cart.class));
    }

    @Test
    void testModifyItemDelete() {
        var cart = new Cart();
        cart.setId(400L);
        cart.setUserId(defaultUser.getId());
        cart.setTotalPrice(200.0);

        var product = new Product();
        product.setId(30L);
        product.setName("Delete Product");
        product.setPrice(100.0);

        var cp1 = new CartProduct(3L, cart.getId(), product.getId());
        var cp2 = new CartProduct(4L, cart.getId(), product.getId());

        when(userRepository.findByUsername("default")).thenReturn(Mono.just(defaultUser));
        when(cartRepository.findByUserId(defaultUser.getId())).thenReturn(Mono.just(cart));
        when(productRepository.findById(product.getId())).thenReturn(Mono.just(product));
        when(cartProductRepository.findAllByCartIdAndProductId(cart.getId(), product.getId()))
                .thenReturn(Flux.just(cp1, cp2));
        when(cartProductRepository.deleteById(cp1.getId())).thenReturn(Mono.empty());
        when(cartProductRepository.deleteById(cp2.getId())).thenReturn(Mono.empty());
        // After deletion, updateCartTotal returns a cart with totalPrice = 0.0
        when(cartProductRepository.findByCartId(cart.getId())).thenReturn(Flux.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        var auth = new UsernamePasswordAuthenticationToken("default", "pass");
        cartService.modifyItem(product.getId(), "delete")
                   .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                   .block();

        verify(cartProductRepository).findAllByCartIdAndProductId(cart.getId(), product.getId());
        verify(cartProductRepository).deleteById(cp1.getId());
        verify(cartProductRepository).deleteById(cp2.getId());
        verify(cartRepository, atLeastOnce()).save(any(Cart.class));
    }
}
