package com.example.productservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_sku_option")
@IdClass(ProductSkuOptionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSkuOption {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private ProductSku sku;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_value_id", nullable = false)
    private ProductOptionValue optionValue;
}
