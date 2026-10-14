package com.example.market.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
@TableName("orders")
public class Order {
    @TableId(type=IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long buyerId;
    private Long sellerId;
    private Long productId;
    private String productTitleSnapshot;
    private BigDecimal priceSnapshot;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long value) { id=value; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String value) { orderNo=value; }
    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long value) { buyerId=value; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long value) { sellerId=value; }
    public Long getProductId() { return productId; }
    public void setProductId(Long value) { productId=value; }
    public String getProductTitleSnapshot() { return productTitleSnapshot; }
    public void setProductTitleSnapshot(String value) { productTitleSnapshot=value; }
    public BigDecimal getPriceSnapshot() { return priceSnapshot; }
    public void setPriceSnapshot(BigDecimal value) { priceSnapshot=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { createdAt=value; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime value) { updatedAt=value; }
}
