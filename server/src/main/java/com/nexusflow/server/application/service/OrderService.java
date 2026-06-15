package com.nexusflow.server.application.service;

import com.nexusflow.server.domain.model.Order;
import com.nexusflow.server.domain.model.OrderEvent;
import com.nexusflow.server.domain.model.OrderItem;
import com.nexusflow.server.domain.model.OrderStatus;
import com.nexusflow.server.domain.model.PaymentStatus;
import com.nexusflow.server.domain.model.Product;
import com.nexusflow.server.domain.model.User;
import com.nexusflow.server.exception.BusinessRuleException;
import com.nexusflow.server.exception.InsufficientStockException;
import com.nexusflow.server.exception.ResourceNotFoundException;
import com.nexusflow.server.infrastructure.messaging.OrderOutboxService;
import com.nexusflow.server.infrastructure.persistence.OrderRepository;
import com.nexusflow.server.infrastructure.persistence.ProductRepository;
import com.nexusflow.server.infrastructure.persistence.UserRepository;
import com.nexusflow.server.web.dto.OrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String SYSTEM_ACTOR = "system";
    private static final String ASYNC_WORKER_ACTOR = "async-order-worker";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderStateMachine stateMachine;
    private final OrderOutboxService orderOutboxService;
    private final OrderMetrics orderMetrics;

    @Transactional
    public OrderDto.OrderResponse createOrder(
            OrderDto.OrderRequest request,
            String username,
            String idempotencyKey,
            String requestCorrelationId) {
        User user = requireUser(username);
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);

        if (normalizedKey != null) {
            var existing = orderRepository.findByUserIdAndIdempotencyKey(user.getId(), normalizedKey);
            if (existing.isPresent()) {
                log.info("Returning idempotent order result orderId={} user={} key={}",
                        existing.get().getId(), username, normalizedKey);
                return OrderDto.OrderResponse.from(existing.get());
            }
        }

        String correlationId = StringUtils.hasText(requestCorrelationId)
                ? requestCorrelationId.trim()
                : UUID.randomUUID().toString();
        log.info("Creating order user={} correlationId={}", username, correlationId);

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.CREATED)
                .paymentStatus(PaymentStatus.PENDING)
                .idempotencyKey(normalizedKey)
                .correlationId(correlationId)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderDto.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with ID: " + itemRequest.getProductId()));

            int requestedQuantity = itemRequest.getQuantity();
            if (product.availableQuantity() < requestedQuantity) {
                throw new InsufficientStockException(
                        "Insufficient stock for product: " + product.getName()
                                + ". Available: " + product.availableQuantity()
                                + ", Requested: " + requestedQuantity);
            }

            product.reserve(requestedQuantity);
            productRepository.save(product);

            order.addItem(OrderItem.builder()
                    .product(product)
                    .quantity(requestedQuantity)
                    .price(product.getPrice())
                    .build());
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(requestedQuantity)));
        }

        order.setTotalAmount(totalAmount);
        recordEvent(order, "ORDER_CREATED", null, OrderStatus.CREATED, username, correlationId,
                "Order accepted and stock reservation started");
        transition(order, OrderStatus.VALIDATED, SYSTEM_ACTOR, correlationId, "Stock reserved and order validated", false);

        Order savedOrder = orderRepository.save(order);
        orderMetrics.recordCreated();
        log.info("Order created orderId={} user={} total={} correlationId={}",
                savedOrder.getId(), username, totalAmount, correlationId);

        return OrderDto.OrderResponse.from(savedOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto.OrderResponse> findOrders(
            String username,
            OrderStatus status,
            String customer,
            Pageable pageable) {
        User currentUser = requireUser(username);
        boolean staff = currentUser.getRole().isStaff();

        Specification<Order> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        spec = spec
                .and(staff ? null : belongsTo(currentUser))
                .and(status == null ? null : hasStatus(status))
                .and(staff && StringUtils.hasText(customer) ? customerContains(customer) : null);

        return orderRepository.findAll(spec, pageable).map(OrderDto.OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderDto.OrderResponse getOrder(Long orderId, String username) {
        return OrderDto.OrderResponse.from(requireVisibleOrder(orderId, username));
    }

    @Transactional
    public OrderDto.OrderResponse processPayment(
            Long orderId,
            OrderDto.PaymentRequest request,
            String username,
            String idempotencyKey) {
        Order order = requireVisibleOrder(orderId, username);
        if (order.getStatus() != OrderStatus.VALIDATED) {
            throw new BusinessRuleException("Payment can only be processed for VALIDATED orders");
        }

        String correlationId = StringUtils.hasText(idempotencyKey) ? idempotencyKey.trim() : UUID.randomUUID().toString();
        if (request.isApproved()) {
            order.getItems().forEach(item -> item.getProduct().commitReservation(item.getQuantity()));
            order.setPaymentStatus(PaymentStatus.CAPTURED);
            transition(order, OrderStatus.PAID, username, correlationId, "Payment captured", true);
        } else {
            order.getItems().forEach(item -> item.getProduct().releaseReservation(item.getQuantity()));
            order.setPaymentStatus(PaymentStatus.DECLINED);
            transition(order, OrderStatus.FAILED, username, correlationId, "Payment declined", true);
        }

        return OrderDto.OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderDto.OrderResponse updateStatus(
            Long orderId,
            OrderDto.StatusUpdateRequest request,
            String username,
            String correlationId) {
        User actor = requireUser(username);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!actor.getRole().isStaff() && !order.getUser().getId().equals(actor.getId())) {
            throw new ResourceNotFoundException("Order not found with ID: " + orderId);
        }
        if (!actor.getRole().isStaff() && request.getStatus() != OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Users can only cancel their own orders");
        }
        if (request.getStatus() == OrderStatus.PAID) {
            throw new BusinessRuleException("Use the payment endpoint to move an order to PAID");
        }

        String resolvedCorrelationId = StringUtils.hasText(correlationId) ? correlationId.trim() : UUID.randomUUID().toString();
        transition(order, request.getStatus(), username, resolvedCorrelationId, request.getNote(), true);
        return OrderDto.OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public void advancePaidOrderWorkflow(Long orderId, String correlationId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (order.getStatus() != OrderStatus.PAID) {
            log.info("Skipping async processing for orderId={} status={} correlationId={}",
                    orderId, order.getStatus(), correlationId);
            return;
        }

        transition(order, OrderStatus.PROCESSING, ASYNC_WORKER_ACTOR, correlationId,
                "Warehouse processing started", true);
        transition(order, OrderStatus.SHIPPED, ASYNC_WORKER_ACTOR, correlationId,
                "Order handed to carrier", true);
        orderRepository.save(order);
    }

    private void transition(
            Order order,
            OrderStatus toStatus,
            String actor,
            String correlationId,
            String note,
            boolean publishEvent) {
        OrderStatus fromStatus = order.getStatus();
        stateMachine.assertTransition(fromStatus, toStatus);
        adjustInventoryForTerminalTransition(order, fromStatus, toStatus);
        order.setStatus(toStatus);
        order.setCorrelationId(correlationId);
        recordEvent(order, "ORDER_STATUS_CHANGED", fromStatus, toStatus, actor, correlationId, note);
        orderMetrics.recordTransition(toStatus);

        if (publishEvent) {
            orderOutboxService.enqueueStatusChanged(order.getId(), fromStatus, toStatus, actor, correlationId);
        }
    }

    private void adjustInventoryForTerminalTransition(Order order, OrderStatus fromStatus, OrderStatus toStatus) {
        if (toStatus != OrderStatus.CANCELLED && toStatus != OrderStatus.FAILED) {
            return;
        }

        if (fromStatus == OrderStatus.CREATED || fromStatus == OrderStatus.VALIDATED) {
            order.getItems().forEach(item -> item.getProduct().releaseReservation(item.getQuantity()));
        } else if (fromStatus == OrderStatus.PAID) {
            order.getItems().forEach(item -> item.getProduct().restock(item.getQuantity()));
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }
    }

    private void recordEvent(
            Order order,
            String eventType,
            OrderStatus fromStatus,
            OrderStatus toStatus,
            String actor,
            String correlationId,
            String note) {
        order.addEvent(OrderEvent.builder()
                .eventType(eventType)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .actor(actor)
                .correlationId(correlationId)
                .note(note)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private Order requireVisibleOrder(Long orderId, String username) {
        User user = requireUser(username);
        if (user.getRole().isStaff()) {
            return orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        }
        return orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return null;
        }
        if (idempotencyKey.length() > 120) {
            throw new BusinessRuleException("Idempotency-Key must be at most 120 characters");
        }
        return idempotencyKey.trim();
    }

    private Specification<Order> belongsTo(User user) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("user").get("id"), user.getId());
    }

    private Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    private Specification<Order> customerContains(String customer) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(
                criteriaBuilder.lower(root.get("user").get("username")),
                "%" + customer.toLowerCase() + "%");
    }
}
