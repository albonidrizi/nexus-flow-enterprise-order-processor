package com.nexusflow.server.config;

import com.nexusflow.server.domain.model.Product;
import com.nexusflow.server.domain.model.Role;
import com.nexusflow.server.domain.model.User;
import com.nexusflow.server.infrastructure.persistence.ProductRepository;
import com.nexusflow.server.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdmin();
        seedManager();
        seedUser();
        seedProducts();
    }

    private void seedAdmin() {
        userRepository.findByUsername("admin").ifPresentOrElse(
                user -> log.info("Admin user already exists"),
                () -> {
                    User admin = User.builder()
                            .username("admin")
                            .password(passwordEncoder.encode("admin123"))
                            .role(Role.ROLE_ADMIN)
                            .build();
                    userRepository.save(admin);
                    log.info("Seeded default admin user (admin/admin123)");
                }
        );
    }

    private void seedManager() {
        userRepository.findByUsername("manager").ifPresentOrElse(
                user -> log.info("Manager user already exists"),
                () -> {
                    User manager = User.builder()
                            .username("manager")
                            .password(passwordEncoder.encode("manager123"))
                            .role(Role.ROLE_MANAGER)
                            .build();
                    userRepository.save(manager);
                    log.info("Seeded default manager user (manager/manager123)");
                }
        );
    }

    private void seedUser() {
        userRepository.findByUsername("user").ifPresentOrElse(
                user -> log.info("Demo user already exists"),
                () -> {
                    User demoUser = User.builder()
                            .username("user")
                            .password(passwordEncoder.encode("user12345"))
                            .role(Role.ROLE_USER)
                            .build();
                    userRepository.save(demoUser);
                    log.info("Seeded default user (user/user12345)");
                }
        );
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            log.info("Products already seeded");
            return;
        }

        List<Product> products = List.of(
                Product.builder().name("Laptop Pro 14").price(new BigDecimal("1899.00")).quantity(15).reservedQuantity(0).build(),
                Product.builder().name("Noise-Canceling Headphones").price(new BigDecimal("299.00")).quantity(40).reservedQuantity(0).build(),
                Product.builder().name("4K Monitor 27\"").price(new BigDecimal("449.00")).quantity(25).reservedQuantity(0).build(),
                Product.builder().name("Mechanical Keyboard").price(new BigDecimal("129.00")).quantity(35).reservedQuantity(0).build()
        );

        productRepository.saveAll(products);
        log.info("Seeded sample products");
    }
}
