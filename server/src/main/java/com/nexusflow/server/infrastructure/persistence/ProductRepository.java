package com.nexusflow.server.infrastructure.persistence;

import com.nexusflow.server.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
