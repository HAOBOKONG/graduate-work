package com.example.market.vo;
import java.util.List;
public record MyOrdersVO(List<OrderVO> purchases, List<OrderVO> sales) {}
