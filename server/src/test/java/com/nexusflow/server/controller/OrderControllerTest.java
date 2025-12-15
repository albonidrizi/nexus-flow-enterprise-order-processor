package com.nexusflow.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.server.dto.OrderDto;
import com.nexusflow.server.entity.*;
import com.nexusflow.server.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

        @MockBean
        private com.nexusflow.server.config.JwtAuthenticationFilter jwtAuthenticationFilter;

    private Order order;
    private OrderDto.OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .build();

        Product product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(BigDecimal.TEN)
                .build();

        OrderItem orderItem = OrderItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .price(BigDecimal.TEN)
                .build();

        order = Order.builder()
                .id(1L)
                .user(user)
                .items(Collections.singletonList(orderItem))
                .totalAmount(BigDecimal.valueOf(20))
                .status(OrderStatus.CONFIRMED)
                .build();

        OrderDto.OrderItemRequest itemRequest = new OrderDto.OrderItemRequest(1L, 2);
        orderRequest = new OrderDto.OrderRequest(Collections.singletonList(itemRequest));
    }

    @Test
    @WithMockUser(username = "testuser")
    void createOrder_Success() throws Exception {
        when(orderService.createOrder(any(OrderDto.OrderRequest.class), eq("testuser")))
                .thenReturn(order);

        mockMvc.perform(post("/api/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(20));
    }

    @Test
    @WithMockUser(username = "testuser")
    void createOrder_ValidationError_EmptyItems() throws Exception {
        OrderDto.OrderRequest emptyRequest = new OrderDto.OrderRequest(Collections.emptyList());

        mockMvc.perform(post("/api/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getMyOrders_Success() throws Exception {
        List<Order> orders = Arrays.asList(order);
        when(orderService.getUserOrders("testuser")).thenReturn(orders);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }
}
