package com.example.productservice.product.domain;

/**
 * 재고 변동 유형
 */
public enum StockChangeType {
    /** 상품 등록 시 초기 재고 */
    INITIAL,
    /** 주문으로 인한 재고 차감 */
    DEDUCTION,
    /** 주문 취소로 인한 재고 복구 */
    RESTORE,
    /** 관리자 수동 수정 */
    MANUAL_EDIT
}
