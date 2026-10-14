package com.example.market.vo;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
public record ProductVO(Long id, Long sellerId, Long categoryId, String title, String description,
    BigDecimal price, BigDecimal originalPrice, Integer conditionLevel, String status,
    OffsetDateTime createdAt, OffsetDateTime updatedAt, String image, List<String> images) {}
