package com.example.productservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_sku_option",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sku_id", "option_value_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSkuOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sku_option_id")
    private Long skuOptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private ProductSku sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_value_id", nullable = false)
    private ProductOptionValue optionValue;
}
