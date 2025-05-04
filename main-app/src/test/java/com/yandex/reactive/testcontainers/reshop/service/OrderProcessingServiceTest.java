package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Cart;
import com.yandex.reactive.testcontainers.reshop.domain.entity.CartProduct;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Order;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.exception.PaymentException;
import com.yandex.reactive.testcontainers.reshop.exception.ResourceNotFoundException;
import com.yandex.reactive.testcontainers.reshop.repository.CartProductRepository;
import com.yandex.reactive.testcontainers.reshop.repository.OrderProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;

    @Mock
    private ProductService productService;

    @Mock
    private OrderProductRepository orderProductRepository;

    @Mock
    private CartProductRepository cartProductRepository;

    @Mock
    private PaymentClientService paymentClientService;

    @InjectMocks
    private OrderProcessingService underTest;

    @Test
    void testProcessOrder_Success() {
        var cart = new Cart();
        cart.setId(1L);
        cart.setUserId(1L);
        cart.setTotalPrice(100.0);

        when(cartService.getCart()).thenReturn(Mono.just(cart));

        var cp1 = new CartProduct();
        cp1.setProductId(10L);
        var cp2 = new CartProduct();
        cp2.setProductId(10L);
        var cp3 = new CartProduct();
        cp3.setProductId(20L);

        when(cartProductRepository.findByCartId(anyLong()))
                .thenReturn(Flux.just(cp1, cp2, cp3));

        var product10 = new Product();
        product10.setId(10L);
        product10.setPrice(30.0);

        var product20 = new Product();
        product20.setId(20L);
        product20.setPrice(40.0);

        when(productService.findById(10L)).thenReturn(Mono.just(product10));
        when(productService.findById(20L)).thenReturn(Mono.just(product20));

        var orderFromDB = new Order();
        orderFromDB.setId(5L);
        orderFromDB.setUserId(cart.getUserId());
        orderFromDB.setTotalSum(cart.getTotalPrice());
        when(orderService.save(any(Order.class))).thenReturn(Mono.just(orderFromDB));

        when(paymentClientService.makePayment(String.valueOf(cart.getUserId()), cart.getTotalPrice()))
                .thenReturn(Mono.just(true));

        when(cartService.clearCart()).thenReturn(Mono.empty());

        Order result = underTest.processOrder().block();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getUserId()).isEqualTo(cart.getUserId());
    }

    @Test
    void testProcessOrder_EmptyCartProducts() {
        var cart = new Cart();
        cart.setId(1L);
        cart.setUserId(1L);
        cart.setTotalPrice(0.0);

        when(cartService.getCart()).thenReturn(Mono.just(cart));
        when(cartProductRepository.findByCartId(anyLong())).thenReturn(Flux.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> underTest.processOrder().block()
        );
        assertThat(exception.getMessage()).isEqualTo("Cart is empty");
    }

    @Test
    void testProcessOrder_PaymentFailure() {
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUserId(1L);
        cart.setTotalPrice(100.0);
        when(cartService.getCart()).thenReturn(Mono.just(cart));

        CartProduct cp = new CartProduct();
        cp.setProductId(10L);
        when(cartProductRepository.findByCartId(anyLong()))
                .thenReturn(Flux.just(cp));

        Product product = new Product();
        product.setId(10L);
        product.setPrice(100.0);
        when(productService.findById(10L)).thenReturn(Mono.just(product));

        // Simulate payment failure
        when(paymentClientService.makePayment(String.valueOf(cart.getUserId()), cart.getTotalPrice()))
                .thenReturn(Mono.just(false));

        assertThatThrownBy(() -> underTest.processOrder().block())
                .isInstanceOf(PaymentException.class)
                .hasMessage("Payment failed due to processing error.");
    }
}