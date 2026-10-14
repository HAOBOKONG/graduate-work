package com.example.market.vo;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
public record OrderVO(Long id, String orderNo, Long buyerId, Long sellerId, Long productId,
    String productTitleSnapshot, BigDecimal priceSnapshot, String status,
    OffsetDateTime createdAt, OffsetDateTime updatedAt, OrderUserVO buyer, OrderUserVO seller) {
    public record OrderUserVO(Long id, String username, String nickname) {}
}
