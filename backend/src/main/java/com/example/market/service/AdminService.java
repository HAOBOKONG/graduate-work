package com.example.market.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.market.entity.Product;
import com.example.market.entity.ProductImage;
import com.example.market.entity.User;
import com.example.market.mapper.ProductMapper;
import com.example.market.mapper.ProductImageMapper;
import com.example.market.mapper.UserMapper;
import com.example.market.vo.PageVO;
import com.example.market.vo.ProductVO;
import com.example.market.vo.UserVO;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminService {
    private final ProductMapper products;
    private final ProductImageMapper images;
    private final UserMapper users;
    public AdminService(ProductMapper products, ProductImageMapper images, UserMapper users) {
        this.products=products; this.images=images; this.users=users;
    }
    public PageVO<ProductVO> products(int page,int size,boolean pending) {
        checkPage(page,size);
        QueryWrapper<Product> query=new QueryWrapper<>();
        if(pending) query.eq("status","PENDING_REVIEW");
        long total=products.selectCount(query);
        List<Product> rows=products.selectList(query.orderByDesc("created_at","id").last(limit(page,size)));
        Map<Long,List<String>> urls=new HashMap<>();
        if(!rows.isEmpty()) images.selectList(new QueryWrapper<ProductImage>()
            .in("product_id",rows.stream().map(Product::getId).toList()).orderByAsc("sort_order","id"))
            .forEach(image -> urls.computeIfAbsent(image.getProductId(),key -> new ArrayList<>()).add(image.getImageUrl()));
        return new PageVO<>(rows.stream().map(p -> {
            List<String> pictures=urls.getOrDefault(p.getId(),List.of());
            return new ProductVO(p.getId(),p.getSellerId(),p.getCategoryId(),p.getTitle(),p.getDescription(),p.getPrice(),
                p.getOriginalPrice(),p.getConditionLevel(),p.getStatus(),p.getCreatedAt(),p.getUpdatedAt(),
                pictures.isEmpty()?null:pictures.get(0),pictures);
        }).toList(),total,page,size);
    }
    @Transactional
    public void review(Long id,boolean approved) {
        if(products.selectById(id)==null) throw fail(HttpStatus.NOT_FOUND,"商品不存在");
        int changed=products.update(null,new UpdateWrapper<Product>().eq("id",id).eq("status","PENDING_REVIEW")
            .set("status",approved?"ON_SALE":"REJECTED"));
        if(changed!=1) throw fail(HttpStatus.CONFLICT,"只能审核待审核商品，请刷新后重试");
    }
    @Transactional
    public void deleteProduct(Long id) {
        // 锁定商品，避免删除与购买同时发生。外键约束保留已有交易和关联记录。
        Product product=products.selectOne(new QueryWrapper<Product>().eq("id",id).last("FOR UPDATE"));
        if(product==null) throw fail(HttpStatus.NOT_FOUND,"商品不存在");
        if(List.of("RESERVED","SOLD").contains(product.getStatus())) {
            throw fail(HttpStatus.CONFLICT,"商品涉及交易，不能删除，请保留交易记录");
        }
        try {
            images.delete(new QueryWrapper<ProductImage>().eq("product_id",id));
            products.deleteById(id);
        } catch(DataIntegrityViolationException exception) {
            throw fail(HttpStatus.CONFLICT,"商品存在订单或其他关联记录，不能删除");
        }
    }
    public PageVO<UserVO> users(int page,int size) {
        checkPage(page,size);
        long total=users.selectCount(null);
        List<UserVO> records=users.selectList(new QueryWrapper<User>().orderByDesc("created_at","id").last(limit(page,size)))
            .stream().map(UserVO::from).toList();
        return new PageVO<>(records,total,page,size);
    }
    @Transactional
    public void setUserEnabled(Long id,boolean enabled) {
        User user=users.selectById(id);
        if(user==null) throw fail(HttpStatus.NOT_FOUND,"用户不存在");
        if("ADMIN".equals(user.getRole())) throw fail(HttpStatus.FORBIDDEN,"管理员账号不可禁用或修改状态");
        int changed=users.update(null,new UpdateWrapper<User>().eq("id",id).eq("role","USER")
            .set("status",enabled?"ACTIVE":"DISABLED"));
        if(changed!=1) throw fail(HttpStatus.CONFLICT,"用户状态已变化，请刷新后重试");
    }
    private void checkPage(int page,int size) {
        if(page<1 || size<1 || size>100) throw fail(HttpStatus.BAD_REQUEST,"页码至少1，每页数量为1至100");
    }
    private String limit(int page,int size) { return "LIMIT "+size+" OFFSET "+((long)(page-1)*size); }
    private ResponseStatusException fail(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
}
