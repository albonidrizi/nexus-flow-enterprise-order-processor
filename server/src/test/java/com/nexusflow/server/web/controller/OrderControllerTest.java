package com.nexusflow.server.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.server.application.service.OrderService;
import com.nexusflow.server.domain.model.OrderStatus;
import com.nexusflow.server.domain.model.PaymentStatus;
import com.nexusflow.server.security.JwtService;
import com.nexusflow.server.web.dto.OrderDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private OrderDto.OrderResponse orderResponse;
    private OrderDto.OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        orderResponse = OrderDto.OrderResponse.builder()
                .id(1L)
                .customer("testuser")
                .totalAmount(BigDecimal.valueOf(20))
                .status(OrderStatus.VALIDATED)
                .paymentStatus(PaymentStatus.PENDING)
                .correlationId("corr-1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(List.of(OrderDto.OrderItemResponse.builder()
                        .productId(1L)
                        .productName("Test Product")
                        .quantity(2)
                        .unitPrice(BigDecimal.TEN)
                        .lineTotal(BigDecimal.valueOf(20))
                        .build()))
                .history(List.of())
                .build();

        orderRequest = new OrderDto.OrderRequest(List.of(new OrderDto.OrderItemRequest(1L, 2)));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void createOrderReturnsCreatedDto() throws Exception {
        when(orderService.createOrder(any(OrderDto.OrderRequest.class), eq("testuser"), eq("idem-1"), eq("corr-1")))
                .thenReturn(orderResponse);

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .header("Idempotency-Key", "idem-1")
                        .header("X-Correlation-Id", "corr-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.items[0].productName").value("Test Product"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void createOrderValidationRejectsEmptyItems() throws Exception {
        OrderDto.OrderRequest emptyRequest = new OrderDto.OrderRequest(List.of());

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getOrdersReturnsPagedResults() throws Exception {
        when(orderService.findOrders(eq("manager"), eq(OrderStatus.VALIDATED), eq("test"), any()))
                .thenReturn(new PageImpl<>(List.of(orderResponse), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/orders")
                        .param("status", "VALIDATED")
                        .param("customer", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].status").value("VALIDATED"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void processPaymentReturnsUpdatedOrder() throws Exception {
        OrderDto.OrderResponse paidOrder = OrderDto.OrderResponse.builder()
                .id(1L)
                .customer("testuser")
                .totalAmount(BigDecimal.valueOf(20))
                .status(OrderStatus.PAID)
                .paymentStatus(PaymentStatus.CAPTURED)
                .items(List.of())
                .history(List.of())
                .build();
        when(orderService.processPayment(eq(1L), any(OrderDto.PaymentRequest.class), eq("testuser"), eq("pay-1")))
                .thenReturn(paidOrder);

        mockMvc.perform(post("/api/orders/1/payments")
                        .with(csrf())
                        .header("Idempotency-Key", "pay-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderDto.PaymentRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentStatus").value("CAPTURED"));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void updateStatusAcceptsManagerAction() throws Exception {
        OrderDto.OrderResponse shipped = OrderDto.OrderResponse.builder()
                .id(1L)
                .customer("testuser")
                .status(OrderStatus.SHIPPED)
                .paymentStatus(PaymentStatus.CAPTURED)
                .items(List.of())
                .history(List.of())
                .build();

        when(orderService.updateStatus(eq(1L), any(OrderDto.StatusUpdateRequest.class), eq("manager"), eq("corr-2")))
                .thenReturn(shipped);

        mockMvc.perform(patch("/api/orders/1/status")
                        .with(csrf())
                        .header("X-Correlation-Id", "corr-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderDto.StatusUpdateRequest(OrderStatus.SHIPPED, "Manual carrier update"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    void getOrdersRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }
}
