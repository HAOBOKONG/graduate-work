package com.example.market.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
@TableName("categories")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long parentId;
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long value) { parentId = value; }
}
