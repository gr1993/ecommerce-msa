package com.example.orderservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "할인 적용 요청")
@Getter
@NoArgsConstructor
public class DiscountRequest {

	@Schema(description = "할인 유형 (COUPON, POLICY)", example = "COUPON")
	@NotBlank(message = "할인 유형은 필수입니다")
	private String discountType;

	@Schema(description = "참조 ID (userCouponId 등)", example = "1")
	@NotNull(message = "참조 ID는 필수입니다")
	private Long referenceId;

	@Schema(description = "할인명", example = "10% 할인 쿠폰")
	@NotBlank(message = "할인명은 필수입니다")
	private String discountName;

	@Schema(description = "할인 금액", example = "5000")
	@NotNull(message = "할인 금액은 필수입니다")
	@Positive(message = "할인 금액은 0보다 커야 합니다")
	private Long discountAmount;

	@Schema(description = "할인율 (%)", example = "10.00")
	private BigDecimal discountRate;

	@Schema(description = "할인 설명")
	private String description;

	@Builder
	public DiscountRequest(String discountType, Long referenceId, String discountName,
						   Long discountAmount, BigDecimal discountRate, String description) {
		this.discountType = discountType;
		this.referenceId = referenceId;
		this.discountName = discountName;
		this.discountAmount = discountAmount;
		this.discountRate = discountRate;
		this.description = description;
	}
}
