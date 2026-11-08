package com.example.productservice.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse implements Serializable {
    private Long id;
    private String name;
    private String description;
    private BigDecimal originalPrice;
    private BigDecimal promotionalPrice;
    private BigDecimal currentPrice;
    private boolean onPromotion;
}
