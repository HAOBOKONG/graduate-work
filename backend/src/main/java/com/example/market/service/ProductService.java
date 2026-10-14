package com.example.market.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.market.dto.ProductRequest;
import com.example.market.entity.*;
import com.example.market.mapper.*;
import com.example.market.vo.*;
import java.net.URI;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {
    private final ProductMapper products;
    private final ProductImageMapper images;
    private final CategoryMapper categories;
    private final UserMapper users;
    private final AuthService auth;
    private final ImageUploadService uploads;
    public ProductService(ProductMapper products, ProductImageMapper images, CategoryMapper categories,
                          UserMapper users, AuthService auth, ImageUploadService uploads) {
        this.products=products; this.images=images; this.categories=categories; this.users=users; this.auth=auth; this.uploads=uploads;
    }
    public Long userId(String subject) { return auth.currentUser(subject).id(); }

    public PageVO<ProductVO> list(int page, int size, String keyword, Long categoryId, Long sellerId) {
        if (page < 1 || size < 1 || size > 100) throw fail(HttpStatus.BAD_REQUEST, "页码至少1，每页数量为1至100");
        if (keyword != null && keyword.length() > 200) throw fail(HttpStatus.BAD_REQUEST, "关键词最多200字");
        if (categoryId != null && categoryId < 1) throw fail(HttpStatus.BAD_REQUEST, "分类ID必须大于0");
        QueryWrapper<Product> query = new QueryWrapper<>();
        if (sellerId == null) query.eq("status", "ON_SALE");
        else query.eq("seller_id", sellerId);
        if (categoryId != null) query.eq("category_id", categoryId);
        if (keyword != null && !keyword.isBlank()) {
            // 参数绑定，按字面值匹配，避免将用户的%或_解释为通配符。
            query.apply("(POSITION(LOWER({0}) IN LOWER(title)) > 0 OR POSITION(LOWER({0}) IN LOWER(description)) > 0)", keyword.trim());
        }
        long total=products.selectCount(query);
        query.orderByDesc("created_at", "id").last("LIMIT " + size + " OFFSET " + ((long)(page-1)*size));
        List<Product> rows=products.selectList(query);
        Map<Long,List<String>> pictureMap=pictureMap(rows);
        return new PageVO<>(rows.stream().map(p -> view(p, pictureMap.getOrDefault(p.getId(), List.of()))).toList(), total,page,size);
    }
    public ProductDetailVO detail(Long id) {
        Product product=find(id);
        User seller=users.selectById(product.getSellerId());
        Category category=categories.selectById(product.getCategoryId());
        return new ProductDetailVO(view(product, pictureMap(List.of(product)).getOrDefault(id,List.of())),
            new ProductDetailVO.SellerVO(seller.getId(),seller.getUsername(),seller.getNickname(),seller.getAvatar()),
            new CategoryVO(category.getId(),category.getName(),category.getParentId()));
    }
    @Transactional
    public ProductDetailVO create(ProductRequest request, Long sellerId) {
        validate(request);
        Product product=new Product();
        apply(product,request);
        product.setSellerId(sellerId);
        product.setStatus("PENDING_REVIEW");
        products.insert(product);
        saveImages(product.getId(),request.imageUrls());
        return detail(product.getId());
    }
    @Transactional
    public ProductDetailVO update(Long id, ProductRequest request, Long sellerId) {
        owned(id,sellerId);
        validate(request);
        int changed=products.update(null, new UpdateWrapper<Product>().eq("id",id).eq("seller_id",sellerId)
            .in("status","ON_SALE","OFF_SHELF","PENDING_REVIEW","REJECTED").set("status","PENDING_REVIEW").set("title",request.title().trim())
            .set("description",request.description().trim()).set("category_id",request.categoryId())
            .set("price",request.price()).set("original_price",request.originalPrice())
            .set("condition_level",request.conditionLevel()));
        if (changed != 1) throw fail(HttpStatus.CONFLICT,"当前商品状态不能修改");
        images.delete(new QueryWrapper<ProductImage>().eq("product_id",id));
        saveImages(id,request.imageUrls());
        return detail(id);
    }
    @Transactional
    public ProductDetailVO offShelf(Long id, Long sellerId) {
        owned(id,sellerId);
        int changed=products.update(null,new UpdateWrapper<Product>().eq("id",id).eq("seller_id",sellerId)
            .in("status","ON_SALE","OFF_SHELF").set("status","OFF_SHELF"));
        if (changed != 1) throw fail(HttpStatus.CONFLICT,"当前商品状态不能下架");
        return detail(id);
    }
    private Product find(Long id) {
        Product product=products.selectById(id);
        if (product==null) throw fail(HttpStatus.NOT_FOUND,"商品不存在");
        return product;
    }
    private void owned(Long id, Long sellerId) {
        if (!Objects.equals(find(id).getSellerId(),sellerId)) throw fail(HttpStatus.FORBIDDEN,"只能操作自己发布的商品");
    }
    private void validate(ProductRequest request) {
        if (categories.selectById(request.categoryId())==null) throw fail(HttpStatus.BAD_REQUEST,"分类不存在");
        if (request.imageUrls()!=null) for (String url:request.imageUrls()) {
            if (uploads.isManagedImage(url)) continue;
            try {
                URI uri=URI.create(url);
                if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost()==null || uri.getUserInfo()!=null) throw new IllegalArgumentException();
            } catch (IllegalArgumentException exception) { throw fail(HttpStatus.BAD_REQUEST,"图片地址无效，请重新上传"); }
        }
    }
    private void apply(Product p,ProductRequest r) {
        p.setTitle(r.title().trim()); p.setDescription(r.description().trim()); p.setCategoryId(r.categoryId());
        p.setPrice(r.price()); p.setOriginalPrice(r.originalPrice()); p.setConditionLevel(r.conditionLevel());
    }
    private void saveImages(Long id,List<String> urls) {
        if (urls==null) return;
        for(int i=0;i<urls.size();i++) {
            ProductImage image=new ProductImage(); image.setProductId(id); image.setImageUrl(urls.get(i)); image.setSortOrder(i);
            images.insert(image);
        }
    }
    private Map<Long,List<String>> pictureMap(List<Product> rows) {
        Map<Long,List<String>> result=new HashMap<>();
        if (rows.isEmpty()) return result;
        images.selectList(new QueryWrapper<ProductImage>().in("product_id",rows.stream().map(Product::getId).toList())
            .orderByAsc("sort_order","id")).forEach(image -> result.computeIfAbsent(image.getProductId(),key -> new ArrayList<>()).add(image.getImageUrl()));
        return result;
    }
    private ProductVO view(Product p,List<String> urls) {
        return new ProductVO(p.getId(),p.getSellerId(),p.getCategoryId(),p.getTitle(),p.getDescription(),p.getPrice(),
            p.getOriginalPrice(),p.getConditionLevel(),p.getStatus(),p.getCreatedAt(),p.getUpdatedAt(),
            urls.isEmpty()?null:urls.get(0),urls);
    }
    private ResponseStatusException fail(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
}
