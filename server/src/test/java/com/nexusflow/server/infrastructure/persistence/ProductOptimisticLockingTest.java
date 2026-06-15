package com.nexusflow.server.infrastructure.persistence;

import com.nexusflow.server.domain.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "seed.enabled=false"
})
@ActiveProfiles("test")
class ProductOptimisticLockingTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void throwsOnConcurrentUpdateAndPreservesLatestCommittedValue() {
        Product product = productRepository.save(Product.builder()
                .name("Concurrent Widget")
                .price(new BigDecimal("10.00"))
                .quantity(10)
                .reservedQuantity(0)
                .build());

        Product first = productRepository.findById(product.getId()).orElseThrow();
        Product second = productRepository.findById(product.getId()).orElseThrow();

        entityManager.detach(second);

        first.setQuantity(9);
        productRepository.saveAndFlush(first);

        second.setQuantity(8);
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> productRepository.saveAndFlush(second));

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getQuantity()).isEqualTo(9);
        assertThat(reloaded.getVersion()).isNotNull();
    }
}
