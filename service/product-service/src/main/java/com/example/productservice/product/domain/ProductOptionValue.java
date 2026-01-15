package com.example.productservice.product.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_option_value")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOptionValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_value_id")
    private Long optionValueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_group_id", nullable = false)
    private ProductOptionGroup optionGroup;

    @Column(name = "option_value_name", nullable = false, length = 100)
    private String optionValueName;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "optionValue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductSkuOption> skuOptions = new ArrayList<>();
}
