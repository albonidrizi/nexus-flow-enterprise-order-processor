package com.nexusflow.server.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Version
    private Long version;

    @PrePersist
    void prePersist() {
        if (reservedQuantity == null) {
            reservedQuantity = 0;
        }
    }

    public int availableQuantity() {
        return safe(quantity) - safe(reservedQuantity);
    }

    public void reserve(int amount) {
        reservedQuantity = safe(reservedQuantity) + amount;
    }

    public void releaseReservation(int amount) {
        reservedQuantity = Math.max(0, safe(reservedQuantity) - amount);
    }

    public void commitReservation(int amount) {
        reservedQuantity = Math.max(0, safe(reservedQuantity) - amount);
        quantity = Math.max(0, safe(quantity) - amount);
    }

    public void restock(int amount) {
        quantity = safe(quantity) + amount;
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
