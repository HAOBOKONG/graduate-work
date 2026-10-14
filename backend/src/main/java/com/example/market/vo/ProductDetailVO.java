package com.example.market.vo;
public record ProductDetailVO(ProductVO product, SellerVO seller, CategoryVO category) {
    public record SellerVO(Long id, String username, String nickname, String avatar) {}
}
