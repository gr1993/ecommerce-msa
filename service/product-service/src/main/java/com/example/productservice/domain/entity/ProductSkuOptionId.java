package com.example.productservice.domain.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProductSkuOptionId implements Serializable {

    private Long sku;
    private Long optionValue;
}
