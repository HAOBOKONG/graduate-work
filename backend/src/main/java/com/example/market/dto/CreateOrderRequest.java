package com.example.market.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
/** 买家、卖家、价格、状态均由后端确定。 */
public record CreateOrderRequest(@NotNull(message="请选择商品") @Positive(message="商品ID不合法") Long productId) {}
