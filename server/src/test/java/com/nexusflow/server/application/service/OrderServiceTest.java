package com.nexusflow.server.application.service;

import com.nexusflow.server.domain.model.Order;
import com.nexusflow.server.domain.model.OrderItem;
import com.nexusflow.server.domain.model.OrderStatus;
import com.nexusflow.server.domain.model.PaymentStatus;
import com.nexusflow.server.domain.model.Product;
import com.nexusflow.server.domain.model.Role;
import com.nexusflow.server.domain.model.User;
import com.nexusflow.server.exception.BusinessRuleException;
import com.nexusflow.server.exception.InsufficientStockException;
import com.nexusflow.server.infrastructure.messaging.OrderOutboxService;
import com.nexusflow.server.infrastructure.persistence.OrderRepository;
import com.nexusflow.server.infrastructure.persistence.ProductRepository;
import com.nexusflow.server.infrastructure.persistence.UserRepository;
import com.nexusflow.server.web.dto.OrderDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderOutboxService orderOutboxService;
    @Mock
    private OrderMetrics orderMetrics;

    private OrderService orderService;
    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                productRepository,
                userRepository,
                new OrderStateMachine(),
                orderOutboxService,
                orderMetrics);

        user = User.builder().id(1L).username("testuser").role(Role.ROLE_USER).build();
        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(BigDecimal.TEN)
                .quantity(100)
                .reservedQuantity(0)
                .build();
    }

    @Test
    void createOrderReservesStockAndReturnsValidatedOrder() {
        OrderDto.OrderRequest request = new OrderDto.OrderRequest(
                List.of(new OrderDto.OrderItemRequest(1L, 5)));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdAndIdempotencyKey(1L, "order-key")).thenReturn(Optional.empty());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(123L);
            return order;
        });

        OrderDto.OrderResponse result = orderService.createOrder(request, "testuser", "order-key", "corr-1");

        assertThat(result.getId()).isEqualTo(123L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.VALIDATED);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("50");
        assertThat(result.getHistory()).hasSize(2);
        assertThat(product.getQuantity()).isEqualTo(100);
        assertThat(product.getReservedQuantity()).isEqualTo(5);

        verify(orderMetrics).recordCreated();
        verify(orderMetrics).recordTransition(OrderStatus.VALIDATED);
        verify(orderOutboxService, never()).enqueueStatusChanged(any(), any(), any(), any(), any());
    }

    @Test
    void createOrderReturnsExistingOrderForSameIdempotencyKey() {
        Order existing = Order.builder()
                .id(99L)
                .user(user)
                .status(OrderStatus.VALIDATED)
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(BigDecimal.TEN)
                .items(List.of())
                .events(List.of())
                .idempotencyKey("order-key")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdAndIdempotencyKey(1L, "order-key")).thenReturn(Optional.of(existing));

        OrderDto.OrderResponse result = orderService.createOrder(
                new OrderDto.OrderRequest(List.of(new OrderDto.OrderItemRequest(1L, 1))),
                "testuser",
                "order-key",
                "corr-1");

        assertThat(result.getId()).isEqualTo(99L);
        verify(productRepository, never()).findById(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderRejectsInsufficientStockWithoutSavingOrder() {
        OrderDto.OrderRequest request = new OrderDto.OrderRequest(
                List.of(new OrderDto.OrderItemRequest(1L, 101)));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class,
                () -> orderService.createOrder(request, "testuser", null, "corr-1"));

        assertThat(product.getQuantity()).isEqualTo(100);
        assertThat(product.getReservedQuantity()).isZero();
        verify(orderRepository, never()).save(any());
    }

    @Test
    void processPaymentCapturesPaymentCommitsStockAndPublishesEvent() {
        Order order = validatedOrder();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(123L, 1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDto.OrderResponse result = orderService.processPayment(
                123L,
                new OrderDto.PaymentRequest(true),
                "testuser",
                "payment-key");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(product.getQuantity()).isEqualTo(95);
        assertThat(product.getReservedQuantity()).isZero();
        verify(orderOutboxService).enqueueStatusChanged(
                eq(123L), eq(OrderStatus.VALIDATED), eq(OrderStatus.PAID), eq("testuser"), eq("payment-key"));
    }

    @Test
    void processPaymentRejectsOrderThatIsNotValidated() {
        Order order = validatedOrder();
        order.setStatus(OrderStatus.CREATED);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(123L, 1L)).thenReturn(Optional.of(order));

        assertThrows(BusinessRuleException.class,
                () -> orderService.processPayment(123L, new OrderDto.PaymentRequest(true), "testuser", null));

        verify(orderOutboxService, never()).enqueueStatusChanged(any(), any(), any(), any(), any());
    }

    private Order validatedOrder() {
        Order order = Order.builder()
                .id(123L)
                .user(user)
                .status(OrderStatus.VALIDATED)
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(new BigDecimal("50"))
                .build();
        order.addItem(OrderItem.builder()
                .product(product)
                .quantity(5)
                .price(BigDecimal.TEN)
                .build());
        product.reserve(5);
        return order;
    }
}
