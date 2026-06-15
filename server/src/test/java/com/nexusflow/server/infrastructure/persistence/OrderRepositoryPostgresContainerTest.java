package com.nexusflow.server.infrastructure.persistence;

import com.nexusflow.server.domain.model.Order;
import com.nexusflow.server.domain.model.OrderStatus;
import com.nexusflow.server.domain.model.PaymentStatus;
import com.nexusflow.server.domain.model.Role;
import com.nexusflow.server.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class OrderRepositoryPostgresContainerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("nexusflow_test")
            .withUsername("nexus")
            .withPassword("nexus");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void persistsAndFindsOrderByUserAndIdempotencyKeyInPostgres() {
        User user = userRepository.save(User.builder()
                .username("postgres-user")
                .password("{noop}password")
                .role(Role.ROLE_USER)
                .build());

        Order order = orderRepository.save(Order.builder()
                .user(user)
                .status(OrderStatus.VALIDATED)
                .paymentStatus(PaymentStatus.PENDING)
                .idempotencyKey("idem-postgres-1")
                .correlationId("corr-postgres-1")
                .totalAmount(new BigDecimal("42.00"))
                .build());

        assertThat(orderRepository.findByUserIdAndIdempotencyKey(user.getId(), "idem-postgres-1"))
                .contains(order);
        assertThat(orderRepository.countByStatus(OrderStatus.VALIDATED)).isEqualTo(1);
    }
}
