package com.example.market.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record FavoriteProductVO(Long id, String title, BigDecimal price, String image,
                                OffsetDateTime createdAt) {}
