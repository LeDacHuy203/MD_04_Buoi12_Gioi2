package com.example.productservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal promotionalPrice;

    @Transient
    public BigDecimal getEffectivePrice() {
        return promotionalPrice != null ? promotionalPrice : originalPrice;
    }
}
